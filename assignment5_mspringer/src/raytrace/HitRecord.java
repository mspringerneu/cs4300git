package raytrace;

import org.joml.Vector3f;
import java.util.Comparator;

/**
 * Created by mspringer on 11/21/16.
 */
public class HitRecord {
    private float t;
    private Vector3f intersection;
    private Vector3f normal;
    private util.Material material;

    public HitRecord(float t, Vector3f intersection, Vector3f normal, util.Material material) {
        this.t = t;
        this.intersection = intersection;
        this.normal = normal;
        this.material = material;
    }

    public float getT() {
        return this.t;
    }

    /*Comparator for sorting a list of HitRecords by t*/
    public static Comparator<HitRecord> HitRecordTimeComparator = new Comparator<HitRecord>() {

        public int compare(HitRecord h1, HitRecord h2) {
            float t1 = h1.getT();
            float t2 = h2.getT();

            //ascending order
            return (int) (t1 - t2);
        }
    };
}
