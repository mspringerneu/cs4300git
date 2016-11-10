package sgraph;

import com.jogamp.opengl.GL3;
import org.joml.Matrix4f;
import util.IVertexData;
import util.PolygonMesh;

import java.util.*;

/**
 * A specific implementation of this scene graph. This implementation is still independent
 * of the rendering technology (i.e. OpenGL)
 * @author Amit Shesh
 */
public class Scenegraph<VertexType extends IVertexData> implements IScenegraph<VertexType>
{
    /**
     * The root of the scene graph tree
     */
    protected INode root;

    /**
     * A map to store the (name,mesh) pairs. A map is chosen for efficient search
     */
    protected Map<String,util.PolygonMesh<VertexType>> meshes;

    /**
     * A map to store the (name,node) pairs. A map is chosen for efficient search
     */
    protected Map<String,INode> nodes;

    protected Map<String,String> textures;

    /**
     * The associated renderer for this scene graph. This must be set before attempting to
     * render the scene graph
     */
    protected IScenegraphRenderer renderer;


    public Scenegraph()
    {
        root = null;
        meshes = new HashMap<String,util.PolygonMesh<VertexType>>();
        nodes = new HashMap<String,INode>();
        textures = new HashMap<String,String>();
    }

    public void dispose()
    {
        renderer.dispose();
    }

    /**
     * Sets the renderer, and then adds all the meshes to the renderer.
     * This function must be called when the scene graph is complete, otherwise not all of its
     * meshes will be known to the renderer
     * @param renderer The {@link IScenegraphRenderer} object that will act as its renderer
     * @throws Exception
     */
    @Override
    public void setRenderer(IScenegraphRenderer renderer) throws Exception {
        this.renderer = renderer;

        //now add all the meshes
        for (String meshName:meshes.keySet())
        {
            this.renderer.addMesh(meshName,meshes.get(meshName));
        }

    }


    /**
     * Set the root of the scenegraph, and then pass a reference to this scene graph object
     * to all its node. This will enable any node to call functions of its associated scene graph
     * @param root
     */

    @Override
    public void makeScenegraph(INode root)
    {
        this.root = root;
        this.root.setScenegraph(this);

    }

    /**
     * Draw this scene graph. It delegates this operation to the renderer
     * @param modelView
     */
    @Override
    public void draw(Stack<Matrix4f> modelView) {
        if ((root!=null) && (renderer!=null))
        {
            renderer.draw(root,modelView);
        }
    }


    @Override
    public void addPolygonMesh(String name, util.PolygonMesh<VertexType> mesh)
    {
        meshes.put(name,mesh);
    }




    @Override
    public void animate(float time) {
        int numberOfCars = 5;
        int cartLength = 140;
        float trackRadius = 800f;
        float sineWaveMagnitude = 100f;
        float sineWaveFrequency = 10f;
        float cosWaveMagnitude = 45f;
        float wheelSpeed = 20f;



        for (int i = 0; i < numberOfCars; i++) {
            String carName = "car" + i;

            INode carTransformNode = nodes.get(carName + "-transform");
            INode frontWheelTransformNode = nodes.get(carName + "-front-wheels-offset");
            INode middleWheelTransformNode = nodes.get(carName + "-middle-wheels-offset");
            INode backWheelTransformNode = nodes.get(carName + "-back-wheels-offset");


            float carSpeed = 0.1f;

            float atan = (float)Math.atan((double)((cartLength/2) / trackRadius));
            float rot = (2 * i) - 1;
            float angle = (float)Math.toRadians(time) - (atan * rot);

            Matrix4f transform = new Matrix4f()
                    // move car up/down based on sine wave
                    .translate(0, sineWaveMagnitude * (float)Math.sin(sineWaveFrequency * (double)angle), 0)
                    // progress car along circular orbit about y
                    .rotate(angle, 0, 1, 0)
                    // rotate car about x depending on increasing/decreasing height
                    .rotate((float)Math.toRadians(cosWaveMagnitude * Math.cos(sineWaveFrequency * (double) angle)), 1, 0, 0)
                    // move car from origin along x axis by RADIUS units
                    .translate(trackRadius, 0, 0)
                    // rotate car 180 about Y so it faces the correct direction
                    .rotate((float)Math.toRadians(180), 0, 1, 0);

            carTransformNode.setTransform(transform);
            frontWheelTransformNode.setAnimationTransform(new Matrix4f()
                    .translate(0, 0, 10)
                    .rotate((float)Math.toRadians(wheelSpeed * time), 1, 0, 0)
                    .translate(0, 0, -10));
            middleWheelTransformNode.setAnimationTransform(new Matrix4f()
                    .translate(0, 0, -40)
                    .rotate((float)Math.toRadians(wheelSpeed * time), 1, 0, 0)
                    .translate(0, 0, 40));
            backWheelTransformNode.setAnimationTransform(new Matrix4f()
                    .translate(0, 0, -90)
                    .rotate((float)Math.toRadians(wheelSpeed * time), 1, 0, 0)
                    .translate(0, 0, 90));
            time += carSpeed;

        }

    }

    @Override
    public void addNode(String name, INode node) {
        nodes.put(name,node);
    }


    @Override
    public INode getRoot() {
        return root;
    }

    @Override
    public Map<String, PolygonMesh<VertexType>> getPolygonMeshes() {
       Map<String,util.PolygonMesh<VertexType>> meshes = new HashMap<String,PolygonMesh<VertexType>>(this.meshes);
        return meshes;
    }

    @Override
    public Map<String, INode> getNodes() {
        Map<String,INode> nodes = new TreeMap<String,INode>();
        nodes.putAll(this.nodes);
        return nodes;
    }

    @Override
    public void addTexture(String name, String path) {
        textures.put(name,path);
    }



}
