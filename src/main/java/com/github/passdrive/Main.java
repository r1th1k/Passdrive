package com.github.passdrive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.github.passdrive.Environment.EnvironmentImpl;
import com.github.passdrive.pendrive.PasswordManager;
import com.github.passdrive.protector.configProtection.configurePendrive;
import com.github.passdrive.protector.configProtection.detectConfiguration;
import com.github.passdrive.usbDetector.UsbDevice;
import com.github.passdrive.utils.Result;
import com.github.passdrive.utils.constants.AppConstants;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// Thread2: Manager
// Thread3: Chrome + Extension
public class Main {
    // Chrome extention native app host
    // entry point
    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Boolean isDetected = (Boolean)EnvironmentImpl.getEnvironmentMap("detected");
        Boolean isLogged = (Boolean)EnvironmentImpl.getEnvironmentMap("logged");

        if (isDetected && !isLogged) {
            UsbDevice usb = (UsbDevice)EnvironmentImpl.getEnvironmentMap("usbdevice");
            // Read message from Chrome
            String input = reader.readLine();

            // Parse JSON message
            JsonElement jsonElement = JsonParser.parseString(input);
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            // data: {"type": "", "data": }}
            String requestType = jsonObject.get("type").getAsString();

            if ( "POST".equals(requestType.split("_")[0]) ) {
                if ( requestType.contains("login") ) {
                    // Login
                    JsonObject data = jsonObject.get("data").getAsJsonObject();
                    String masterPassword = data.get("master").getAsString();
                    
                    // Check Configuration
                    if (detectConfiguration.detect(usb)) {
                        // Check Master Password
                        if (PasswordManager.checkMasterPassword(usb, masterPassword)) {
                            EnvironmentImpl.setEnvironmentMap("logged", Boolean.TRUE);
                            isLogged = true;
                            System.out.println(new Result(AppConstants.LOGGEDIN, "Logged in successfully").toJson().toString());
                        } else {
                            System.out.println(new Result(AppConstants.UNAUTHORIZED, "Unauthorized").toJson().toString());
                        }
                    } else {
                        if (configurePendrive.configure(usb, masterPassword)) {
                            // Check Master Password
                            if (PasswordManager.checkMasterPassword(usb, masterPassword)) {
                                EnvironmentImpl.setEnvironmentMap("logged", Boolean.TRUE);
                                isLogged = true;
                                System.out.println(new Result(AppConstants.LOGGEDIN, "Logged in successfully").toJson().toString());
                            } else {
                                System.out.println(new Result(AppConstants.UNAUTHORIZED, "Unauthorized").toJson().toString());
                            }
                        } else {
                            System.out.println(new Result(AppConstants.FAILURE, "Configuration failed").toJson().toString());
                        }
                    }
                }
            }
        }

        if (isDetected && isLogged && !((Boolean)EnvironmentImpl.getEnvironmentMap("removed"))){
            UsbDevice usb = (UsbDevice)EnvironmentImpl.getEnvironmentMap("usbdevice");
            if ( usb.isRemoved() ) {
                // TODO: Send message to Thread1 to start detection again

                EnvironmentImpl.setEnvironmentMap("removed", Boolean.TRUE);
                System.out.println(new Result(AppConstants.LOGGEDOUT, "Logged out successfully").toJson().toString());
            }

            // Read message from Chrome
            String input = reader.readLine();

            // Parse JSON message
            JsonElement jsonElement = JsonParser.parseString(input);
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            // data: {"type": "", "data": }}
            String requestType = jsonObject.get("type").getAsString();

            if ( "POST".equals(requestType.split("_")[0]) ) {
                if ( requestType.contains("domain") ) {
                    // Set domain password
                    JsonObject data = jsonObject.get("data").getAsJsonObject();
                    String domainName = data.get("domain").getAsString();
                    String subDomainName = data.get("subdomain").getAsString();
                    String domainPassword = data.get("password").getAsString();
                    
                    if (PasswordManager.storePassword(usb, domainName, subDomainName, domainPassword)) {
                        System.out.println(new Result(AppConstants.SUCCESS, "Domain password set successfully").toJson().toString());
                    } else {
                        System.out.println(new Result(AppConstants.FAILURE, "Domain password set failed").toJson().toString());
                    }
                }
            }
            
            if ( "GET".equals(requestType.split("_")[0]) ) {
                if ( requestType.contains("domain") ) {
                    // Get domain password
                    JsonObject data = jsonObject.get("data").getAsJsonObject();
                    String domainName = data.get("domain").getAsString();
                    String subDomainName = data.get("subdomain").getAsString();
                    
                    String domainPassword = PasswordManager.getPassword(usb, domainName, subDomainName);
                    if ( domainPassword != null ) {
                        JsonObject response = new JsonObject();
                        response.addProperty("data", domainPassword);
                        System.out.println(new Result(AppConstants.SUCCESS, response.getAsString()).toJson().toString());
                    } else {
                        System.out.println(new Result(AppConstants.FAILURE, "Domain password not found").toJson().toString());
                    }
                
                }
            }

            if ( "DELETE".equals(requestType.split("_")[0]) ) {
                if ( requestType.contains("domain") ) {
                    // Delete domain password
                    JsonObject data = jsonObject.get("data").getAsJsonObject();
                    String domainName = data.get("domain").getAsString();
                    String subDomainName = data.get("subdomain").getAsString();
                    
                    if (PasswordManager.removePassword(usb, domainName, subDomainName)) {
                        System.out.println(new Result(AppConstants.SUCCESS, "Domain password deleted successfully").toJson().toString());
                    } else {
                        System.out.println(new Result(AppConstants.FAILURE, "Domain password delete failed").toJson().toString());
                    }
                }
            }

            if ( "PUT".equals(requestType.split("_")[0]) ) {
                if ( requestType.contains("domain") ) {
                    // Update domain password
                    JsonObject data = jsonObject.get("data").getAsJsonObject();
                    String domainName = data.get("domain").getAsString();
                    String subDomainName = data.get("subdomain").getAsString();
                    String domainPassword = data.get("password").getAsString();
                    
                    if (PasswordManager.updatePassword(usb, domainName, subDomainName, domainPassword)) {
                        System.out.println(new Result(AppConstants.SUCCESS, "Domain password updated successfully").toJson().toString());
                    } else {
                        System.out.println(new Result(AppConstants.FAILURE, "Domain password update failed").toJson().toString());
                    }
                }
            }
        }
    }
}