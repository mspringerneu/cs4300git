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
        this.start = new Vector4f(start.x, start.y, start.z, 1);
        this.direction = new Vector4f(
                (i-(width/2)) - start.x,
                (j-(height/2)) - start.y,
                (float)((-0.5f * height)/Math.tan(theta/2f)) - start.z, 0);
        this.direction.normalize();
    }

    public Ray3D(Vector4f start, Vector4f direction) {
        this.start = new Vector4f(start);
        this.direction = new Vector4f(direction);
        this.direction.normalize();
    }

    public Ray3D(Ray3D ray) {
        this.start = new Vector4f(ray.getStart());
        this.direction = new Vector4f(ray.getDirection());
        this.direction.normalize();
    }

    public Vector4f getStart() {
        return new Vector4f(this.start);
    }

    public Vector4f getDirection() {
        return new Vector4f(this.direction);
    }

    public void viewToWorld(Matrix4f transform) {
        Matrix4f t = new Matrix4f();
        transform.invert(t);
        this.start = t.transform(this.start);
        this.direction = t.transform(this.direction);
        this.direction.normalize();
    }
}
