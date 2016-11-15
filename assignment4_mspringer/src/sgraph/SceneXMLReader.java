package sgraph;

import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;

import org.joml.Matrix4f;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import util.IVertexData;
import util.TextureImage;
import util.VertexProducer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.TreeMap;


/**
 * A SAX parser for parsing the scene graph and compiling an {@link
 * IScenegraph} object from it.
 *
 * @author Amit Shesh
 */
public class SceneXMLReader {
    public static <K extends IVertexData> IScenegraph<K>
    importScenegraph(InputStream in, util.VertexProducer<K> vProducer)
            throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = null;
        IScenegraph<K> scenegraph = null;

        parser = factory.newSAXParser();

        MyHandler handler = new MyHandler(vProducer);
        parser.parse(in, handler);

        scenegraph = handler.getScenegraph();
        return scenegraph;
    }
}

class MyHandler<K extends IVertexData> extends DefaultHandler {
    private util.VertexProducer<? extends IVertexData> vProducer;
    private IScenegraph<K> scenegraph;
    private INode node;
    private Stack<INode> stackNodes;
    private String data;
    private Matrix4f transform;
    private util.Material material;
    private util.Light light;
    private util.TextureImage texture;
    private boolean inLight;
    private boolean inMaterial;
    private Map<String, INode> subgraph;

    public IScenegraph<K> getScenegraph() {
        return scenegraph;
    }

    public MyHandler(util.VertexProducer<K> vProducer) {
        this.vProducer = vProducer;
    }

    public void startDocument() throws SAXException {
        System.out.println("Parsing started");
        node = null;
        stackNodes = new Stack<INode>();
        scenegraph = new Scenegraph<K>();
        subgraph = new TreeMap<String, INode>();
        transform = new Matrix4f();
        material = new util.Material();
        light = new util.Light();
        inLight = false;
        inMaterial = false;

    }

    public void endDocument() throws SAXException {
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        System.out.println("Start tag: " + qName);
        switch (qName) {
            case "scene": {
                stackNodes.push(new GroupNode(scenegraph, "Root of scene graph"));
                subgraph.put(stackNodes.peek().getName(), stackNodes.peek());
            }
            break;
            case "group": {
                String name = "";
                String copyof = "";
                String fromfile = "";
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name"))
                        name = attributes.getValue(i);
                    else if (attributes.getQName(i).equals("copyof"))
                        copyof = attributes.getValue(i);
                    else if (attributes.getQName(i).equals("from"))
                        fromfile = attributes.getValue(i);
                }
                if ((copyof.length() > 0) && (subgraph.containsKey(copyof))) {
                    node = subgraph.get(copyof).clone();
                    node.setName(name);
                } else if (fromfile.length() > 0) {
                    IScenegraph<K> tempsg = null;
                    try {
                        tempsg = SceneXMLReader.importScenegraph(getClass()
                                .getClassLoader().getResourceAsStream
                                        (fromfile), (VertexProducer<K>) vProducer);
                    } catch (Exception e) {
                        throw new SAXException(e.getMessage());
                    }
                    node = new GroupNode(scenegraph, name);

                    for (Map.Entry<String, util.PolygonMesh<K>> s : tempsg
                            .getPolygonMeshes().entrySet()) {
                        scenegraph.addPolygonMesh(s.getKey(), s.getValue());
                    }
                    //rename all the nodes in tempsg to prepend with the name of the group node
                    Map<String, INode> nodes = tempsg.getNodes();
                    for (Map.Entry<String, INode> s : nodes.entrySet()) {
                        s.getValue().setName(name + "-" + s.getValue().getName());
                        scenegraph.addNode(s.getValue().getName(), s.getValue());
                    }

                    node.addChild(tempsg.getRoot());
                } else
                    node = new GroupNode(scenegraph, name);
                try {
                    stackNodes.peek().addChild(node);
                } catch (IllegalArgumentException e) {
                    throw new SAXException(e.getMessage());
                }
                stackNodes.push(node);
                subgraph.put(stackNodes.peek().getName(), stackNodes.peek());
            }
            break;
            case "transform": {
                String name = "";
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name"))
                        name = attributes.getValue(i);
                }
                node = new TransformNode(scenegraph, name);
                try {
                    stackNodes.peek().addChild(node);
                } catch (IllegalArgumentException e) {
                    throw new SAXException(e.getMessage());
                }
                transform.identity();
                stackNodes.push(node);
                subgraph.put(stackNodes.peek().getName(), stackNodes.peek());
            }
            break;
            case "object": {
                String name = "";
                String objectname = "";
                String texturename = "";
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                    }
                    else if (attributes.getQName(i).equals("instanceof")) {
                        objectname = attributes.getValue(i);
                    }
                    else if (attributes.getQName(i).equals("texture")) {
                        texturename = attributes.getValue(i);
                    }
                }
                if (objectname.length() > 0) {
                    node = new LeafNode(objectname, scenegraph, name);
                    try {
                        stackNodes.peek().addChild(node);
                    } catch (IllegalArgumentException e) {
                        throw new SAXException(e.getMessage());
                    }
                    if (texturename.length() > 0) {
                        node.setTextureName(texturename);
                        stackNodes.push(node);
                        subgraph.put(stackNodes.peek().getName(), stackNodes.peek());
                    }
                    else {
                        node.setTextureName("default");
                        stackNodes.push(node);
                        subgraph.put(stackNodes.peek().getName(), stackNodes.peek());
                    }
                }
            }
            break;
            case "instance": {
                String name = "";
                String path = "";
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("path")) {
                        path = attributes.getValue(i);
                        if (!path.endsWith(".obj")) {
                            path = path.split(".", 1)[0];
                            path = path + ".obj";
                        }
                    }
                }
                if ((name.length() > 0) && (path.contains("models/"))) {
                    util.PolygonMesh<K> mesh = null;
                    mesh = util.ObjImporter.importFile((VertexProducer<K>) vProducer, getClass()
                            .getClassLoader().getResourceAsStream(path), false);
                    scenegraph.addPolygonMesh(name, mesh);
                }
            }
            break;
            case "image": {
                String name = "";
                String path = "";
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("path")) {
                        path = attributes.getValue(i);
                        if (!path.endsWith(".png")) {
                            path = path.split(".", 1)[0];
                            path = path + ".png";
                        }
                    }
                }
                if ((name.length() > 0) && (path.contains("textures/"))) {
                    scenegraph.addTexture(name, path);
                }
            }
            break;
            case "material": {
                this.inMaterial = true;
            }
            break;
            case "light": {
                this.inLight = true;
            }
            break;
        }
        data = "";
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        Scanner sc;
        System.out.println("End tag: " + qName);
        switch (qName) {
            case "scene":
                if (stackNodes.peek().getName().equals("Root of scene graph"))
                    scenegraph.makeScenegraph(stackNodes.peek());
                else
                    throw new SAXException("Invalid scene file");
                break;
            case "group":
            case "transform":
            case "object":
                stackNodes.pop();
                break;
            case "set":
                stackNodes.peek().setTransform(transform);
                transform.identity();
                break;
            case "scale":
                sc = new Scanner(data);
                transform.scale(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
                break;
            case "rotate":
                sc = new Scanner(data);
                transform.rotate((float) Math.toRadians(sc.nextFloat()), sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
                break;
            case "translate":
                sc = new Scanner(data);
                transform.translate(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
                break;
            case "material":
                stackNodes.peek().setMaterial(material);
                material = new util.Material();
                this.inMaterial = false;
                break;
            case "light":
                stackNodes.peek().addLight(light);
                light = new util.Light();
                this.inLight = false;
                break;
            case "color":
                sc = new Scanner(data);
                material.setAmbient(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
                material.setDiffuse(material.getAmbient());
                material.setSpecular(material.getAmbient());
                material.setShininess(1.0f);
                break;
            case "ambient":
                sc = new Scanner(data);
                if (this.inLight) {
                    light.setAmbient(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
                }
                else if (this.inMaterial) {
                    material.setAmbient(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
                }
                break;
            case "diffuse":
                sc = new Scanner(data);
                if (this.inLight) {
                    light.setDiffuse(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
                }
                else if (this.inMaterial) {
                    material.setDiffuse(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
                }
                break;
            case "specular":
                sc = new Scanner(data);
                if (this.inLight) {
                    light.setSpecular(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
                }
                else if (this.inMaterial) {
                    material.setSpecular(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
                }
                break;
            case "emissive":
                sc = new Scanner(data);
                material.setEmission(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
                break;
            case "shininess":
                sc = new Scanner(data);
                material.setShininess(sc.nextFloat());
                break;
            case "absorption":
                sc = new Scanner(data);
                material.setAbsorption(sc.nextFloat());
                break;
            case "reflection":
                sc = new Scanner(data);
                material.setReflection(sc.nextFloat());
                break;
            case "transparency":
                sc = new Scanner(data);
                material.setTransparency(sc.nextFloat());
                break;
            case "refractive":
                sc = new Scanner(data);
                material.setRefractiveIndex(sc.nextFloat());
                break;
            case "position":
                sc = new Scanner(data);
                light.setPosition(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
                break;
            case "spotDirection":
                sc = new Scanner(data);
                light.setSpotDirection(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
                break;
            case "spotAngle":
                sc = new Scanner(data);
                light.setSpotAngle(sc.nextFloat());
                break;
        }
        data = "";
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (data.length() > 0)
            data = data + " " + new String(ch, start, length);
        else
            data = new String(ch, start, length);
    }

}





