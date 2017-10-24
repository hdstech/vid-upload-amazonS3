package com.hellokoding.auth.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.hellokoding.auth.s3.S3Services;
//import com.hellokoding.auth.s3.S3Services;
//import com.hellokoding.auth.s3.S3Services;
import com.hellokoding.auth.storage.StorageFileNotFoundException;
import com.hellokoding.auth.storage.StorageService;

@Controller
public class FileUploadController {
	@Value("${jsa.aws.access_key_id}")
	private String awsId;
 
	@Value("${jsa.aws.secret_access_key}")
	private String awsKey;
	
	@Value("${jsa.s3.region}")
	private String region;
	
	@Value("${jsa.s3.bucket}")
	private String bucketName;
	
    private final StorageService storageService;
    private final S3Services s3Services;
    
	
    @Autowired
    public FileUploadController(StorageService storageService, S3Services s3Services) {
        this.storageService = storageService;
        this.s3Services = s3Services;
    }

    @GetMapping("/user")
    public String listUploadedFiles(Model model) throws IOException {

        model.addAttribute("files", storageService.loadAll().map(
                path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                        "serveFile", path.getFileName().toString()).build().toString())
                .collect(Collectors.toList()));

        return "fileupload";
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/")
    public String handleFileUpload(
    		@RequestParam("file") MultipartFile file,
    		@RequestParam("name") String name,
            RedirectAttributes redirectAttributes) throws IOException {

//        storageService.store(file);
//        String contents = new String(file.getBytes());
//        s3Services.uploadFile(contents, uploadFilePath);
//        redirectAttributes.addFlashAttribute("message",
//                "You successfully uploaded " + file.getOriginalFilename() + "!");
    	BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsId, awsKey);
    	AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
							.withRegion(Regions.fromName(region))
			                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
			                .build();
    	
    	
    	
    	if(!file.getContentType().toLowerCase().equals("video/mp4")) {
    		return "redirect:/fileupload";
    	}
    	
    	InputStream is = file.getInputStream();
    	
    	s3Client.putObject(new PutObjectRequest(bucketName, name, is, new ObjectMetadata()).withCannedAcl(CannedAccessControlList.PublicRead));
    	
    	S3Object s3Object = s3Client.getObject(new GetObjectRequest(bucketName, name));
    	
    	redirectAttributes.addAttribute("picUrl", s3Object.getObjectContent().getHttpRequest().getURI().toString());
    	
        return "redirect:/dashboard";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}