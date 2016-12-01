package raytrace;

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

    public HitRecord(float tEnter, float tExit, Vector4f intersectionIn, Vector4f intersectionOut, Vector4f normalIn, Vector4f normalOut, util.Material material) {
        this.tEnter = tEnter;
        this.tExit = tExit;
        this.intersectionIn = intersectionIn;
        this.intersectionOut = intersectionOut;
        this.normalIn = normalIn;
        this.normalOut = normalOut;
        this.material = material;
    }

    public float getTEnter() {
        return this.tEnter;
    }

    public float getTExit() { return this.tExit; }

    public Vector4f getIntersectionIn() {
        return this.intersectionIn;
    }
    public Vector4f getIntersectionOut() {
        return this.intersectionOut;
    }
    public Vector4f getNormalIn() {
        return this.normalIn;
    }
    public Vector4f getNormalOut() {
        return this.normalOut;
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
