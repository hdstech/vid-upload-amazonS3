package com.hellokoding.auth.s3;

public interface S3Services {
	public void uploadFile(String keyName, String uploadFilePath);
	public void downloadFile(String keyName, String downloadFilePath);
}