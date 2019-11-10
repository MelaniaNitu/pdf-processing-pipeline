package com.example.pdf_processing;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.*;
import org.springframework.boot.context.embedded.tomcat.*;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PdfProcessingApplication<EmbeddedServletContainerCustomizer, ConfigurableEmbeddedServletContainer> {

	public static void main(String[] args) {
		SpringApplication.run(PdfProcessingApplication.class, args);
	    System.out.println("Application started ... launching browser now");
	    Browse("http://localhost:8080");
	}

	
	
	public static void Browse(String url) {
	    if(Desktop.isDesktopSupported()){
	        Desktop desktop = Desktop.getDesktop();
	        try {
	            desktop.browse(new URI(url));
	        } catch (IOException | URISyntaxException e) {
	            e.printStackTrace();
	        }
	    }else{
	        Runtime runtime = Runtime.getRuntime();
	        try {
	            runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	}
	
	//How to edit the default maxPostSize of embedded Tomcat in SpringBoot
	//Add this code to the same class running SpringApplication.run.

/*	@Bean 
	  EmbeddedServletContainerCustomizer containerCustomizer(
	        ) throws Exception {
	 
	      
	      return (ConfigurableEmbeddedServletContainer container) -> {
	 
	          if (container instanceof TomcatEmbeddedServletContainerFactory) {
	 
	              TomcatEmbeddedServletContainerFactory tomcat = (TomcatEmbeddedServletContainerFactory) container;
	              tomcat.addConnectorCustomizers(
	                      (connector) -> {
	                    	  connector.setMaxPostSize(1099511627776);//1
	                      }
	              );
	          }
	      };
	  }
*/
	/*@Bean
	public ServletWebServerFactoryCustomizer customizer() throws Exception{
		
		
		
		return null;
		
		
		
	}*/
	
}

