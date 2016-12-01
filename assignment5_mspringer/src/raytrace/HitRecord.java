package raytrace;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Comparator;

/**
 * Created by mspringer on 11/21/16.
 */
public class HitRecord {
    private float tEnter;
    private float tExit;
    private Vector4f intersectionIn;
    private Vector4f normalIn;
    private Vector4f intersectionOut;
    private Vector4f normalOut;
    private util.Material material;
    private Matrix4f transform;

    public HitRecord(float tEnter, float tExit, Vector4f intersectionIn, Vector4f intersectionOut, Vector4f normalIn, Vector4f normalOut, util.Material material, Matrix4f transform) {
        this.tEnter = tEnter;
        this.tExit = tExit;
        this.intersectionIn = intersectionIn;
        this.intersectionOut = intersectionOut;
        this.normalIn = normalIn;
        this.normalOut = normalOut;
        this.material = material;
        this.transform = transform;
    }

    public float getTEnter() {
        return this.tEnter;
    }

    public float getTExit() { return this.tExit; }

    public Vector4f getIntersectionIn() {
        return new Vector4f(this.intersectionIn);
    }
    public Vector4f getIntersectionOut() {
        return new Vector4f(this.intersectionOut);
    }
    public Vector4f getNormalIn() {
        return new Vector4f(this.normalIn);
    }
    public Vector4f getNormalOut() {
        return new Vector4f(this.normalOut);
    }
    public Matrix4f getTransform() {
        return new Matrix4f(this.transform);
    }

    public util.Material getMaterial() {
        return this.material;
    }

    /*Comparator for sorting a list of HitRecords by t*/
    public static Comparator<HitRecord> HitRecordTimeComparator = new Comparator<HitRecord>() {

        public int compare(HitRecord h1, HitRecord h2) {
            float t1 = h1.getTEnter();
            float t2 = h2.getTEnter();

            //ascending order
            return (int) (t1 - t2);
        }
    };
}
