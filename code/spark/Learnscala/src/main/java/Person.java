public class Person {
    private String firstName;
    private String lastName;
    private final String HOME = System.getProperty("user.home");
    private int age;

    public Person(String firstName, String lastName) {
        super();
        this.firstName = firstName;
        this.lastName = lastName;
        System.out.println("the constructor begins");
        age = 0;
        printHome();
        printFullName();
        System.out.println("still in the constructor");
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public int age() {
        return age;
    }

    public void firstName_$eq(String firstName) {
        this.firstName = firstName;
    }

    public void lastName_$eq(String lastName) {
        this.lastName = lastName;
    }

    public void age_$eq(int age) {
        this.age = age;
    }

    public String toString() {
        return firstName + " " + lastName + " is " + age + " years old";
    }

    public void printHome() {
        System.out.println(HOME);
    }

    public void printFullName() {
        System.out.println(this);
    }
}