package raytrace;

/**
 * Created by mspringer on 11/21/16.
 */

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Ray3D {
    private Vector4f start;
    private Vector4f direction;

    public Ray3D(Vector4f start, int i, int j, int width, int height, float theta) {
        this.start = start;
        this.direction = new Vector4f(
                (i-(width/2)),
                (j-(height/2)),
                (float)((-0.5f * height)/Math.tan(theta/2)), 0).normalize();
    }

    public Ray3D(Vector4f start, Vector4f direction) {
        this.start = start;
        this.direction = direction;
    }

    public Ray3D(Ray3D ray) {
        this.start = ray.getStart();
        this.direction = ray.getDirection();
    }

    public Vector4f getStart() {
        return this.start;
    }

    public Vector4f getDirection() {
        return this.direction;
    }

    public void mul(Matrix4f transform) {
        this.start = transform.invert().transform(this.start);
        this.direction = transform.invert().transform(this.direction).normalize();
    }
}
