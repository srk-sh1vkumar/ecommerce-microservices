package com.ecommerce.monitoring.service;

import com.ecommerce.monitoring.entity.AutomatedFix;
import com.ecommerce.monitoring.entity.ErrorPattern;
import com.ecommerce.monitoring.repository.AutomatedFixRepository;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AutomatedFixingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AutomatedFixingService.class);
    
    @Value("${monitoring.git.repository-url:}")
    private String repositoryUrl;
    
    @Value("${monitoring.git.workspace:/tmp/monitoring-workspace}")
    private String workspacePath;
    
    @Value("${monitoring.git.username:}")
    private String gitUsername;
    
    @Value("${monitoring.git.token:}")
    private String gitToken;
    
    private final AutomatedFixRepository automatedFixRepository;
    private final JavaParser javaParser;
    
    public AutomatedFixingService(AutomatedFixRepository automatedFixRepository) {
        this.automatedFixRepository = automatedFixRepository;
        this.javaParser = new JavaParser();
    }
    
    /**
     * Trigger automated fix for an error pattern
     */
    public void triggerAutomatedFix(ErrorPattern pattern) {
        try {
            logger.info("Triggering automated fix for pattern: {} in service: {}", 
                       pattern.getSignature(), pattern.getServiceName());
            
            AutomatedFix fix = new AutomatedFix(pattern.getId(), pattern.getServiceName(), 
                                              determineFixType(pattern.getErrorType()));
            
            fix.setDescription("Automated fix for " + pattern.getErrorType() + " in " + pattern.getServiceName());
            
            automatedFixRepository.save(fix);
            
            // Execute the fix asynchronously
            executeAutomatedFix(fix, pattern);
            
        } catch (Exception e) {
            logger.error("Error triggering automated fix for pattern: {}", pattern.getSignature(), e);
        }
    }
    
    /**
     * Execute the automated fix
     */
    private void executeAutomatedFix(AutomatedFix fix, ErrorPattern pattern) {
        try {
            // Step 1: Prepare workspace and clone repository
            prepareWorkspace();
            Git git = cloneRepository();
            
            // Step 2: Create feature branch
            String branchName = createFeatureBranch(git, fix);
            fix.setBranchName(branchName);
            
            // Step 3: Analyze and fix code
            boolean fixApplied = analyzeAndFixCode(fix, pattern);
            
            if (fixApplied) {
                // Step 4: Commit changes
                String commitId = commitChanges(git, fix);
                fix.markAsApplied(commitId, branchName);
                
                // Step 5: Run tests
                boolean testsPassed = runTests(fix);
                fix.setTestsPassed(testsPassed);
                
                if (testsPassed) {
                    // Step 6: Create pull request (if configured)
                    createPullRequest(fix);
                    fix.setStatus("tested");
                } else {
                    fix.markAsFailed("Tests failed after applying fix");
                }
            } else {
                fix.markAsFailed("Could not apply automated fix");
            }
            
            automatedFixRepository.save(fix);
            
        } catch (Exception e) {
            logger.error("Error executing automated fix: {}", fix.getId(), e);
            fix.markAsFailed("Exception during fix execution: " + e.getMessage());
            automatedFixRepository.save(fix);
        }
    }
    
    /**
     * Analyze code and apply appropriate fixes
     */
    private boolean analyzeAndFixCode(AutomatedFix fix, ErrorPattern pattern) {
        try {
            String serviceName = pattern.getServiceName();
            String className = pattern.getClassName();
            String methodName = pattern.getMethodName();
            
            // Find the Java file
            Path javaFile = findJavaFile(serviceName, className);
            if (javaFile == null) {
                logger.warn("Could not find Java file for class: {}", className);
                return false;
            }
            
            // Parse the Java file
            CompilationUnit cu = javaParser.parse(javaFile).getResult().orElse(null);
            if (cu == null) {
                logger.warn("Could not parse Java file: {}", javaFile);
                return false;
            }
            
            // Read original content
            String originalContent = Files.readString(javaFile);
            fix.setOriginalCode(originalContent);
            fix.setFilePath(javaFile.toString());
            
            // Apply fix based on error type
            boolean modified = false;
            switch (pattern.getErrorType()) {
                case "NullPointerException":
                    modified = fixNullPointerException(cu, methodName, pattern);
                    break;
                case "SQLException":
                    modified = fixSQLException(cu, methodName, pattern);
                    break;
                case "RestClientException":
                    modified = fixRestClientException(cu, methodName, pattern);
                    break;
                case "BeanCreationException":
                    modified = fixBeanCreationException(cu, className, pattern);
                    break;
                default:
                    logger.warn("No automated fix available for error type: {}", pattern.getErrorType());
                    return false;
            }
            
            if (modified) {
                // Write modified content back to file
                String fixedContent = cu.toString();
                Files.writeString(javaFile, fixedContent);
                fix.setFixedCode(fixedContent);
                
                // Document changes
                List<String> changes = new ArrayList<>();
                changes.add("Applied " + pattern.getErrorType() + " fix");
                changes.add("Modified method: " + methodName);
                fix.setChanges(changes);
                
                return true;
            }
            
        } catch (Exception e) {
            logger.error("Error analyzing and fixing code", e);
        }
        
        return false;
    }
    
    /**
     * Fix NullPointerException by adding null checks and Optional usage
     */
    private boolean fixNullPointerException(CompilationUnit cu, String methodName, ErrorPattern pattern) {
        return cu.findAll(MethodDeclaration.class).stream()
                .filter(method -> method.getNameAsString().equals(methodName))
                .findFirst()
                .map(method -> {
                    try {
                        // Add null checks for method parameters
                        method.getParameters().forEach(param -> {
                            String paramName = param.getNameAsString();
                            String nullCheck = String.format(
                                "if (%s == null) { throw new IllegalArgumentException(\"%s cannot be null\"); }",
                                paramName, paramName
                            );
                            
                            BlockStmt body = method.getBody().orElse(new BlockStmt());
                            // Insert null check at the beginning of method
                            body.addStatement(0, javaParser.parseStatement(nullCheck).getResult().orElse(null));
                            method.setBody(body);
                        });
                        
                        // Add import for IllegalArgumentException if needed
                        cu.addImport("java.lang.IllegalArgumentException", false, false);
                        
                        return true;
                    } catch (Exception e) {
                        logger.error("Error fixing NullPointerException", e);
                        return false;
                    }
                })
                .orElse(false);
    }
    
    /**
     * Fix SQLException by adding @Retryable annotation and circuit breaker
     */
    private boolean fixSQLException(CompilationUnit cu, String methodName, ErrorPattern pattern) {
        return cu.findAll(MethodDeclaration.class).stream()
                .filter(method -> method.getNameAsString().equals(methodName))
                .findFirst()
                .map(method -> {
                    try {
                        // Add @Retryable annotation
                        method.addAnnotation(javaParser.parseAnnotation(
                            "@Retryable(value = {SQLException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))"
                        ).getResult().orElse(null));
                        
                        // Add necessary imports
                        cu.addImport("org.springframework.retry.annotation.Retryable", false, false);
                        cu.addImport("org.springframework.retry.annotation.Backoff", false, false);
                        cu.addImport("java.sql.SQLException", false, false);
                        
                        return true;
                    } catch (Exception e) {
                        logger.error("Error fixing SQLException", e);
                        return false;
                    }
                })
                .orElse(false);
    }
    
    /**
     * Fix RestClientException by adding circuit breaker
     */
    private boolean fixRestClientException(CompilationUnit cu, String methodName, ErrorPattern pattern) {
        return cu.findAll(MethodDeclaration.class).stream()
                .filter(method -> method.getNameAsString().equals(methodName))
                .findFirst()
                .map(method -> {
                    try {
                        // Add @CircuitBreaker annotation
                        method.addAnnotation(javaParser.parseAnnotation(
                            "@CircuitBreaker(name = \"" + pattern.getServiceName() + "\", fallbackMethod = \"" + methodName + "Fallback\")"
                        ).getResult().orElse(null));
                        
                        // Add necessary imports
                        cu.addImport("io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker", false, false);
                        
                        return true;
                    } catch (Exception e) {
                        logger.error("Error fixing RestClientException", e);
                        return false;
                    }
                })
                .orElse(false);
    }
    
    /**
     * Fix BeanCreationException by adding missing annotations
     */
    private boolean fixBeanCreationException(CompilationUnit cu, String className, ErrorPattern pattern) {
        return cu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class).stream()
                .filter(clazz -> clazz.getNameAsString().equals(className))
                .findFirst()
                .map(clazz -> {
                    try {
                        // Check if class needs @Service, @Component, or @Repository annotation
                        boolean hasSpringAnnotation = clazz.getAnnotations().stream()
                                .anyMatch(ann -> ann.getNameAsString().matches("Service|Component|Repository|Controller"));
                        
                        if (!hasSpringAnnotation) {
                            // Add @Service annotation (most common for business logic)
                            clazz.addAnnotation("Service");
                            cu.addImport("org.springframework.stereotype.Service", false, false);
                            return true;
                        }
                        
                        return false;
                    } catch (Exception e) {
                        logger.error("Error fixing BeanCreationException", e);
                        return false;
                    }
                })
                .orElse(false);
    }
    
    // Helper methods for Git operations
    
    private void prepareWorkspace() throws IOException {
        Path workspace = Paths.get(workspacePath);
        if (Files.exists(workspace)) {
            // Clean existing workspace
            deleteDirectory(workspace.toFile());
        }
        Files.createDirectories(workspace);
    }
    
    private Git cloneRepository() throws Exception {
        if (repositoryUrl == null || repositoryUrl.isEmpty()) {
            // Use local repository for development
            return Git.open(new File("."));
        }
        
        return Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(new File(workspacePath))
                .call();
    }
    
    private String createFeatureBranch(Git git, AutomatedFix fix) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String branchName = String.format("automated-fix/%s-%s", fix.getFixType(), timestamp);
        
        git.checkout()
           .setCreateBranch(true)
           .setName(branchName)
           .call();
        
        return branchName;
    }
    
    private String commitChanges(Git git, AutomatedFix fix) throws Exception {
        git.add().addFilepattern(".").call();
        
        String commitMessage = String.format(
            "🤖 Automated fix: %s\n\n" +
            "Service: %s\n" +
            "Fix type: %s\n" +
            "Description: %s\n\n" +
            "Generated by Intelligent Monitoring Service\n" +
            "Fix ID: %s",
            fix.getDescription(),
            fix.getServiceName(),
            fix.getFixType(),
            fix.getDescription(),
            fix.getId()
        );
        
        return git.commit()
                 .setMessage(commitMessage)
                 .setAuthor("Intelligent Monitoring", "monitoring@ecommerce.com")
                 .call()
                 .getId()
                 .getName();
    }
    
    private boolean runTests(AutomatedFix fix) {
        try {
            // Run Maven tests for the specific service
            ProcessBuilder pb = new ProcessBuilder("mvn", "test", "-pl", fix.getServiceName());
            pb.directory(new File(workspacePath));
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            boolean testsPassed = exitCode == 0;
            
            fix.setTestResults(testsPassed ? "All tests passed" : "Tests failed with exit code: " + exitCode);
            
            return testsPassed;
            
        } catch (Exception e) {
            logger.error("Error running tests", e);
            fix.setTestResults("Error running tests: " + e.getMessage());
            return false;
        }
    }
    
    private void createPullRequest(AutomatedFix fix) {
        // This would integrate with GitHub/GitLab API to create PR
        // For now, just log the action
        logger.info("Would create pull request for automated fix: {}", fix.getId());
        fix.setPullRequestUrl("https://github.com/ecommerce/automated-fix-" + fix.getId());
    }
    
    private Path findJavaFile(String serviceName, String className) {
        try {
            String serviceDir = serviceName.replace("-", "");
            Path serviceRoot = Paths.get(workspacePath, serviceDir, "src", "main", "java");
            
            if (!Files.exists(serviceRoot)) {
                serviceRoot = Paths.get(workspacePath, serviceName, "src", "main", "java");
            }
            
            if (Files.exists(serviceRoot)) {
                return Files.walk(serviceRoot)
                           .filter(path -> path.toString().endsWith(className + ".java"))
                           .findFirst()
                           .orElse(null);
            }
            
        } catch (Exception e) {
            logger.error("Error finding Java file for class: {}", className, e);
        }
        
        return null;
    }
    
    private String determineFixType(String errorType) {
        switch (errorType) {
            case "NullPointerException": return "null_check";
            case "SQLException": return "retry_circuit_breaker";
            case "RestClientException": return "circuit_breaker";
            case "BeanCreationException": return "spring_annotation";
            case "OutOfMemoryError": return "memory_optimization";
            default: return "generic_fix";
        }
    }
    
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}