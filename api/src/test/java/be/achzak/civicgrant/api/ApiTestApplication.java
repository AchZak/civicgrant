package be.achzak.civicgrant.api;

import org.springframework.boot.autoconfigure.SpringBootApplication;

// The api module has no application class of its own, and slice tests need one to start from.
@SpringBootApplication(proxyBeanMethods = false)
class ApiTestApplication {}
