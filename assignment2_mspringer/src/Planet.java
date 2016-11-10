import org.joml.Vector4f;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by mspringer on 10/3/16.
 */


public class Planet {
    private int MOON_SPEED = 1;
    public String name;
    private int radius;
    private Vector4f color;
    private int orbitRadius;
    private float phi;
    private float theta;
    private int speed;
    public List<Planet> moons = new ArrayList<Planet>();

    public Planet(String name, int radius, Vector4f color, int orbitRadius, float phi, float theta, int speed) {
        this.name = name;
        this.radius = radius;
        this.color = color;
        this.orbitRadius = orbitRadius;
        this.phi = phi;
        this.theta = theta;
        this.speed = speed;
        this.moons = new ArrayList<Planet>();
    }

    public Planet(String name) {
        switch (name) {
            case "Sun":
                this.name = name;
                this.radius = 70;
                this.color = new Vector4f(1,1,0,1);
                this.orbitRadius = 0;
                this.phi = 0f;
                this.theta = 0f;
                this.speed = 0;
                this.moons = new ArrayList<Planet>();
                break;
            case "Mercury":
                this.name = name;
                this.radius = 10;
                this.color = new Vector4f(1f,(float)105/255,(float)193/255,1f);  // light pink
                this.orbitRadius = 200;
                this.phi = (float)Math.toRadians(30);
                this.theta = (float)Math.toRadians(30);
                this.speed = 3;
                this.moons = new ArrayList<Planet>();
                break;
            case "Venus":
                this.name = name;
                this.radius = 20;
                this.color = new Vector4f(1f,1f,153f/255f,1f);  // light yellow
                this.orbitRadius = 300;
                this.phi = (float)Math.toRadians(10);
                this.theta = (float)Math.toRadians(335);
                this.speed = 5;
                this.moons = new ArrayList<Planet>();
                break;
            case "Earth":
                this.name = name;
                this.radius = 40;
                this.color = new Vector4f(135f/255f,206f/255f,250f/255f,1);  // light blue
                this.orbitRadius = 550;
                this.phi = (float)Math.toRadians(-60);
                this.theta = (float)Math.toRadians(50);
                this.speed = 7;
                this.moons = new ArrayList<Planet>();
                this.moons.add(new Planet("EarthMoon"));
                break;
            case "Jupiter":
                this.name = name;
                this.radius = 60;
                this.color = new Vector4f(1f,165f/255f,0f,1f);  // orange
                this.orbitRadius = 900;
                this.phi = (float)Math.toRadians(60);
                this.theta = (float)Math.toRadians(20);
                this.speed = 10;
                this.moons = new ArrayList<Planet>();
                this.moons.add(new Planet("JupiterMoon1"));
                this.moons.add(new Planet("JupiterMoon2"));
                break;
            case "EarthMoon":
                this.name = name;
                this.radius = 20;
                this.color = new Vector4f(1f,1f,1f,1f);  // white
                this.orbitRadius = 100;
                this.phi = (float)Math.toRadians(20);
                this.theta = (float)Math.toRadians(10);
                this.speed = MOON_SPEED;
                this.moons = new ArrayList<Planet>();
                break;
            case "JupiterMoon1":
                this.name = name;
                this.radius = 20;
                this.color = new Vector4f(1f,1f,1f,1f);  // white
                this.orbitRadius = 120;
                this.phi = (float)Math.toRadians(-80);
                this.theta = (float)Math.toRadians(50);
                this.speed = MOON_SPEED;
                this.moons = new ArrayList<Planet>();
                break;
            case "JupiterMoon2":
                this.name = name;
                this.radius = 20;
                this.color = new Vector4f(1f,1f,1f,1f);  // white
                this.orbitRadius = 120;
                this.phi = (float)Math.toRadians(30);
                this.theta = (float)Math.toRadians(100);
                this.speed = MOON_SPEED;
                this.moons = new ArrayList<Planet>();
                break;
            default:
                throw new IllegalArgumentException("This is not a valid name for a planet, please enter a valid name");
        }
    }

    // returns the radius of the planet
    public int getRadius() {
        return this.radius;
    }

    // returns the color of the planet
    public Vector4f getColor() {
        return this.color;
    }

    // returns the radius of the planet's orbit
    public int getOrbitRadius() {
        return this.orbitRadius;
    }

    // returns the phi rotation of the planet's orbit
    public float getPhi() {
        return this.phi;
    }

    // returns the theta rotation of the planet's orbit
    public float getTheta() {
        return this.theta;
    }

    // returns the speed of the planet
    public int getSpeed() {
        return this.speed;
    }

    // returns true if the planet has any moons
    public boolean hasMoons() {
        return this.moons.size() > 0;
    }
}
