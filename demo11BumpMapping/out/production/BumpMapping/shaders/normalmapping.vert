#version 140



in vec4 vPosition;
in vec4 vNormal;
in vec4 vTexCoord;
in vec4 vTangent;

uniform mat4 projection;
uniform mat4 modelview;
uniform mat4 normalmatrix;
uniform mat4 texturematrix;
out vec3 fNormal;
out vec3 fTangent;
out vec3 fBitangent;
out vec4 fPosition;
out vec4 fTexCoord;

void main()
{
    vec3 lightVec,viewVec,reflectVec;
    vec3 normalView;
    vec3 ambient,diffuse,specular;
    float nDotL,rDotV;

    fPosition = modelview * vec4(vPosition.xyzw);
    gl_Position = projection * fPosition;


    vec4 tNormal = normalmatrix * vNormal;
    fNormal = normalize(tNormal.xyz);

    vec4 tTangent = modelview * vTangent;
    fTangent = normalize(tTangent.xyz);
    fBitangent = cross(fNormal,fTangent);
    fBitangent = normalize(fBitangent);


    fTexCoord = texturematrix * vTexCoord;
}
