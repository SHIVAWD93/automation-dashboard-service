package com.qa.automation.service;

import com.qa.automation.model.Tester;
import com.qa.automation.repository.TesterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class TesterService {
    
    @Autowired
    private TesterRepository testerRepository;
    
    private final String uploadDir = "uploads/";
    
    public List<Tester> getAllTesters() {
        return testerRepository.findAll();
    }
    
    public Tester createTester(Tester tester, MultipartFile profileImage) throws IOException {
        if (profileImage != null && !profileImage.isEmpty()) {
            String fileName = saveProfileImage(profileImage);
            tester.setProfileImageUrl("/uploads/" + fileName);
        }
        return testerRepository.save(tester);
    }
    
    public Tester getTesterById(Long id) {
        return testerRepository.findById(id).orElse(null);
    }
    
    public boolean deleteTester(Long id) {
        if (testerRepository.existsById(id)) {
            testerRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    private String saveProfileImage(MultipartFile file) throws IOException {
        // Create uploads directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + extension;
        
        // Save file
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());
        
        return fileName;
    }
}
