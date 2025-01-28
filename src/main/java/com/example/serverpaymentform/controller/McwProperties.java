package com.example.serverpaymentform.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.springframework.stereotype.Component;

@Component
// Clase usada para manejar la lectura de propiedades del archivo config.properties
public class McwProperties {
    private Properties properties = new Properties();

    public McwProperties() {
	// Obtiene el archivo config.properties desde el classpath
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            // Carga las propiedades desde el InputStream
	   properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    // Obtiene el valor de una propiedad específica
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}
