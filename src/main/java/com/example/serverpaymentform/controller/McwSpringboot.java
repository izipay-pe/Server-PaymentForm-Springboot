package com.example.serverpaymentform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class McwSpringboot {
    
    @Autowired
    private McwProperties properties;

    @Autowired
    private McwController mcwController;
    
    /**
     * @@ Manejo de solicitudes POST@@
     */
    @PostMapping("/formtoken")
    public ResponseEntity<?> createFormToken(@RequestBody Map<String, String> parameters) {
        try {
            // Obtener PublicKey
            String PUBLIC_KEY = properties.getProperty("PUBLIC_KEY");
            
            // Obtenemos el FormToken generado
            String formToken = mcwController.generateFormToken(parameters);
            
            // Crear respuesta
            Map<String, String> response = new HashMap<>();
            response.put("formToken", formToken);
            response.put("publicKey", PUBLIC_KEY);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/validate")
    public ResponseEntity<?> processResult(@RequestBody Map<String, String> resultParameters) {
        try {
            String HMAC_SHA256 = properties.getProperty("HMAC_SHA256");
            
            // Asignando los valores de la respuesta de Izipay
            String krHash = resultParameters.get("kr-hash");
            String krAnswer = resultParameters.get("kr-answer");
            
            // Válida que la respuesta sea íntegra
            if (!mcwController.checkHash(krHash, HMAC_SHA256, krAnswer)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid hash"));
            }

            // Retornarmos la respuesta
            return ResponseEntity.ok("true");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/ipn")
    public ResponseEntity<?> processIpn(@RequestParam Map<String, String> ipnParameters) {
        try {
            String PASSWORD = properties.getProperty("PASSWORD");
            
            // Asignando los valores de la respuesta IPN
            String krHash = ipnParameters.get("kr-hash");
            String krAnswer = ipnParameters.get("kr-answer");
            
            // Válida que la respuesta sea íntegra
            if (!mcwController.checkHash(krHash, PASSWORD, krAnswer)) {
                return ResponseEntity.badRequest().body(Map.of("status", "No valid IPN"));
            }

            // Procesa la respuesta del pago
            JSONObject jsonResponse = new JSONObject(krAnswer);
            JSONArray transactionsArray = jsonResponse.getJSONArray("transactions");
            JSONObject transactions = transactionsArray.getJSONObject(0);
            
            // Verifica el orderStatus PAID
            String orderStatus = jsonResponse.getString("orderStatus");
            String orderId = jsonResponse.getJSONObject("orderDetails").getString("orderId");
            String uuid = transactions.getString("uuid"); 
            
	    return ResponseEntity.ok("OK! Order Status: " + orderStatus);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
