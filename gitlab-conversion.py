import sys, os, re, json, shutil, errno

def transform(root, f, targetdir):
    full = os.path.join(root, f)
    input = open(full, 'r').read()
    dir = os.path.join(targetdir, root)
    if not os.path.exists(dir):
        os.makedirs(dir)
    output = open(os.path.join(dir, f), 'w')
    input = applyTransformation(input)
    output.write(input)


def applyTransformation(input):
    for variable in re.findall(r"\{\{(.*?)\}\}", input):
        tmp = variable.replace('.', '_')
        input = input.replace(variable, tmp)
    input = input.replace('{{', '{').replace('}}', '}')
    input = re.sub(r"<<fake.+#", "<<", input)
    for variable in re.findall(r"[ ]*{% if (.*?) %}", input):
        tmp = variable.replace('.', '_')
        input = input.replace(variable, tmp)
    exp = re.compile("[ ]*{% if (.*?) %}(.*?)[ ]*{% endif %}", re.DOTALL)
    input = re.sub(exp, "ifeval::[{\g<1>}==true]\g<2>endif::[]", input)
    input = re.sub(r"image:(\.\./)*", "image:", input)
    return input


indir = 'topics'
targetdir = 'target'
if len(sys.argv) > 1:
    targetdir = sys.argv[1]

shutil.rmtree(os.path.join(targetdir, 'images'))
shutil.rmtree(os.path.join(targetdir, 'keycloak-images'))
shutil.rmtree(os.path.join(targetdir, 'rhsso-images'))
shutil.copytree('images',os.path.join(targetdir, 'images'))
shutil.copytree('keycloak-images',os.path.join(targetdir, 'keycloak-images'))
shutil.copytree('rhsso-images',os.path.join(targetdir, 'rhsso-images'))

tmp = os.path.join(targetdir, 'topics')
if not os.path.exists(tmp):
    os.makedirs(tmp)

# transform files
for root, dirs, filenames in os.walk(indir):
    for f in filenames:
        transform(root,f,targetdir)

# Create master.doc includes
input = open('SUMMARY.adoc', 'r').read()
output = open(os.path.join(targetdir, 'master.adoc'), 'w')

output.write("""
:toc:
:toclevels: 3
:numbered:

include::document-attributes.adoc[]
""")

input = re.sub(r"[ ]*\.+\s*link:(.*)\[(.*)\]", "include::\g<1>[]", input)
input = applyTransformation(input)
output.write(input)

# parse book.json file and create document attributes
with open('book.json') as data_file:
    data = json.load(data_file)

variables = data['variables']

def makeAttributes(variables, variable, list):
    for i in variables.keys():
        if variable is None:
            tmp = i
        else:
            tmp = variable + '_' + i
        if isinstance(variables[i],dict):
            makeAttributes(variables[i], tmp, list)
        elif isinstance(variables[i],bool):
            boolval = 'false'
            if variables[i]:
                boolval = 'true'
            list.append({tmp: boolval})
        else:
            list.append({tmp: str(variables[i])})


attributeList = []
makeAttributes(variables, None, attributeList)

output = open(os.path.join(targetdir, 'document-attributes.adoc'), 'w')
for attribute in attributeList:
    for k in attribute.keys():
        output.write(':book_' + k + ": " + attribute[k] + "\n")

print "Transformation complete!"









