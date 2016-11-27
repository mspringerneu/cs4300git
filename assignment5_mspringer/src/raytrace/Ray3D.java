package raytrace;

/**
 * Created by mspringer on 11/21/16.
 */

import org.joml.Vector3f;

public class Ray3D {
    private Vector3f start;
    private Vector3f direction;

    public Ray3D(Vector3f start, int i, int j, int width, int height, float theta) {
        this.start = start;
        this.direction = new Vector3f(
                (i-(width/2)) - start.x,
                (j-(height/2)) - start.y,
                (float)(((-0.5f * height)/Math.tan(theta/2)) - start.z));
    }

    public Vector3f getStart() {
        return this.start;
    }

    public Vector3f getDirection() {
        return this.direction;
    }
}
