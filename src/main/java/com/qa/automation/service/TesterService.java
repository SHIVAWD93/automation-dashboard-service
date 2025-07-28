package com.qa.automation.service;

import com.qa.automation.model.Tester;
import com.qa.automation.repository.TesterRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TesterService {

    @Autowired
    private TesterRepository testerRepository;

    public List<Tester> getAllTesters() {
        List<Tester> testers = testerRepository.findAll();

        testers.forEach(tester -> {
            Path imagePath = Paths.get("uploads", tester.getId() + ".png");
            try {
                byte[] imageBytes = Files.readAllBytes(imagePath);
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                tester.setProfileImageUrl(base64Image);  // base64 string instead of URL
            }
            catch (IOException e) {
                // If file not found or error, you can either set null or some default
                tester.setProfileImageUrl(null);
                // optionally log error here
            }
        });

        return testers;
    }


    public Tester createTester(Tester tester) {
        return testerRepository.save(tester);
    }

    public Tester createTester(Tester tester, MultipartFile imageFile) {
        if (tester.getExperience() == null) {
            tester.setExperience(0);
        }
        Tester savedTester = testerRepository.save(tester);
        if (!imageFile.isEmpty()) {
            // Prepare file path
            String uploadDir = "uploads/";
            String fileName = savedTester.getId() + ".png";
            File file = new File(uploadDir + fileName);
            file.getParentFile().mkdirs(); // ensure directory exists
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(imageFile.getBytes());
            }
            catch (IOException e) {
                throw new RuntimeException("Failed to save image", e);
            }
            // Save image path to DB
            savedTester.setProfileImageUrl(uploadDir + fileName);
        }
        return testerRepository.save(savedTester);
    }


    public Tester getTesterById(Long id) {
        return testerRepository.findById(id).orElse(null);
    }

    public Tester updateTester(Long id, Tester tester) {
        if (testerRepository.existsById(id)) {
            tester.setId(id);
            // Set default experience if not provided
            if (tester.getExperience() == null) {
                tester.setExperience(0);
            }
            return testerRepository.save(tester);
        }
        return null;
    }

    public boolean deleteTester(Long id) {
        if (testerRepository.existsById(id)) {
            testerRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Tester> getTestersByRole(String role) {
        return testerRepository.findByRole(role);
    }

    public List<Tester> getTestersByGender(String gender) {
        return testerRepository.findByGender(gender);
    }

    public List<Tester> searchTestersByName(String name) {
        return testerRepository.findByNameContaining(name);
    }

    public List<Tester> getTestersByExperience(Integer minExperience) {
        return testerRepository.findByExperienceGreaterThanEqual(minExperience);
    }

    public long getTestersCountByRole(String role) {
        return testerRepository.countByRole(role);
    }

    public long getTestersCountByGender(String gender) {
        return testerRepository.countByGender(gender);
    }
}