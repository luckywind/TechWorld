[json和pojo互转](https://www.geeksforgeeks.org/convert-java-object-to-json-string-using-jackson-api/)

# 使用Jackson

```xml
<dependency> 
        <groupId>com.fasterxml.jackson.core</groupId> 
        <artifactId>jackson-databind</artifactId> 
        <version>2.5.3</version> 
</dependency> 
```

```java
public class Organisation { 
    private String organisation_name; 
    private String description; 
    private int Employees; 
  
    // Calling getters and setters 
    public String getOrganisation_name() 
    { 
        return organisation_name; 
    } 
  
    public void setOrganisation_name(String organisation_name) 
    { 
        this.organisation_name = organisation_name; 
    } 
  
    public String getDescription() 
    { 
        return description; 
    } 
  
    public void setDescription(String description) 
    { 
        this.description = description; 
    } 
  
    public int getEmployees() 
    { 
        return Employees; 
    } 
  
    public void setEmployees(int employees) 
    { 
        Employees = employees; 
    } 
  
    // Creating toString 
    @Override
    public String toString() 
    { 
        return "Organisation [organisation_name="
            + organisation_name 
            + ", description="
            + description 
            + ", Employees="
            + Employees + "]"; 
    } 
} 
```

## pojo转json

```java
  
import java.io.IOException; 
import org.codehaus.jackson.map.ObjectMapper; 
import com.Geeks.Organisation; 
  
public class ObjectToJson { 
    public static void main(String[] a) 
    { 
  
        // Creating object of Organisation 
        Organisation org = new Organisation(); 
  
        // Insert the data into the object 
        org = getObjectData(org); 
  
        // Creating Object of ObjectMapper define in Jakson Api 
        ObjectMapper Obj = new ObjectMapper(); 
  
        try { 
  
            // get Oraganisation object as a json string 
            String jsonStr = Obj.writeValueAsString(org); 
  
            // Displaying JSON String 
            System.out.println(jsonStr); 
        } 
  
        catch (IOException e) { 
            e.printStackTrace(); 
        } 
    } 
  
    // Get the data to be inserted into the object 
    public static getObjectData(Organisation org) 
    { 
  
        // Insert the data 
        org.setName("GeeksforGeeks"); 
        org.setDescription("A computer Science portal for Geeks"); 
        org.setEmployees(2000); 
  
        // Return the object 
        return org; 
    } 
```

