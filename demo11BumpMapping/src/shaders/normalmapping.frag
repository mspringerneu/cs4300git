#version 140

struct MaterialProperties
{
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float shininess;
};

struct LightProperties
{
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    vec4 position;
};


in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;
in vec4 fPosition;
in vec4 fTexCoord;

const int MAXLIGHTS = 10;

uniform MaterialProperties material;
uniform LightProperties light[MAXLIGHTS];
uniform int numLights;

uniform bool bumpMapping;

/* texture */
uniform sampler2D image;
/*normal map */
uniform sampler2D normalmap;

out vec4 fColor;

void main()
{

    vec3 lightVec,viewVec,reflectVec;
    vec3 ambient,diffuse,specular;
    float nDotL,rDotV;
    vec3 tempNormal,tempTangent,tempBitangent;
    vec3 tNormal;


    tempNormal = normalize(fNormal);
    tempTangent = normalize(fTangent);
    tempBitangent = normalize(fBitangent);


    viewVec = -fPosition.xyz;
    viewVec = normalize(viewVec);

    /* convert viewVec to tangent space */
    viewVec = vec3(dot(viewVec,tempTangent),dot(viewVec,tempBitangent),dot(viewVec,tempNormal));
    viewVec = normalize(viewVec);


    if (bumpMapping)
    {
        /* normal in tangent space obtained from normal map */
        tNormal = texture2D(normalmap,vec2(fTexCoord.s,fTexCoord.t)).rgb;
        //read normal is in range (0,1) in each channel, convert it to (-1,1)
        tNormal = 2* tNormal - 1;
        tNormal = normalize(tNormal);
    }
    else
    {
        tNormal = vec3(0,0,1); //normal in tangent space
    }

    fColor = vec4(0,0,0,1);

    for (int i=0;i<numLights;i++)
    {
        if (light[i].position.w!=0)
            lightVec = normalize(light[i].position.xyz - fPosition.xyz);
        else
            lightVec = normalize(-light[i].position.xyz);

        /* convert lightVec to tangent space */
        lightVec = vec3(dot(lightVec,tempTangent),dot(lightVec,tempBitangent),dot(lightVec,tempNormal));
        lightVec = normalize(lightVec);





        nDotL = dot(tNormal,lightVec);



        reflectVec = reflect(-lightVec,tNormal);
        reflectVec = normalize(reflectVec);

        rDotV = max(dot(reflectVec,viewVec),0.0);

        /*outColor = vZColor; */
        ambient = material.ambient * light[i].ambient;
        diffuse = material.diffuse * light[i].diffuse * max(nDotL,0);
        if (nDotL>0)
            specular = material.specular * light[i].specular * pow(rDotV,material.shininess);
        else
            specular = vec3(0,0,0);
        
		fColor = fColor + vec4(ambient+diffuse+specular,1.0);
			
    }
//    fColor = vec4(fTexCoord.s,fTexCoord.t,0,1);
    fColor = fColor * texture2D(image,vec2(fTexCoord.s,fTexCoord.t));
//
 //  fColor = vec4(tempTangent.xyz,1);
}
