import requests
import json

#COSTANTI
url = "https://api.github.com"
urlMods = url + "/repos/spiritodellaforgia/forgiacraft2/contents/forgiacraft2/mods"
urlBase = url + "/repos/spiritodellaforgia/forgiacraft2/contents"
urlConfig = url + "/repos/spiritodellaforgia/forgiacraft2/contents/forgiacraft2/config"
urlAuth = url + "/repos/d/forgiacraft2"
urlRate = url + "/rate_limit"

user = "spiritodellaforgia@gmail.com"
password = input("Inserisci password")


def getNumberOfRequest():
    authResponse = requests.get(urlRate, auth=(user, password))
    print(authResponse.content)
    jsonResponse = authResponse.content.decode()
    j = json.loads(jsonResponse)
    return j['resources']['core']['limit']

'''
homeUrl: Root del repo e.g. /repos/dariopassarello/ForgiaModPack/contents
pathsToVisit: array che contiene le cartelle che devono essere analizzate ricorsivamente
outputJsonArray: Un array passato inizialmente vuoto 
'''
def getTreeJson(homeUrl,pathsToVisit,outputJsonArray):
    files = 0
    subFolders = 0
    skip = False
    for dirToVisit in pathsToVisit:
        skip = False
        if dirToVisit == "forgiacraft2/mods/hats" or dirToVisit == "forgiacraft2/config/agricraft/json" or dirToVisit == "forgiacraft2/config/biomesoplenty/biomes":
            print("--------------------------------------------")
            print("SKIPPING PATH " + dirToVisit)
            print("--------------------------------------------")
            continue
        jsonTreeResponse = requests.get(homeUrl + dirToVisit + '/',auth=(user, password))
        jsonTreeString  = jsonTreeResponse.content.decode()
        jsonTreeArray = json.loads(jsonTreeString)
        print(jsonTreeArray)
        for element in jsonTreeArray:
            if element['type'] == 'file':
                elementStruct = {}
                print("Found file: ",element['path'])
                files = files + 1
                elementStruct['path'] = element['path']
                elementStruct['download_url'] = element['download_url']
                elementStruct['size'] = element['size']
                elementStruct['hash'] = element['sha']
                outputJsonArray['files'].append(elementStruct)
            else:
                subFolders = subFolders + 1
                newPaths = []
                print("Found folder: ",element['path'])
                newPaths.append(element['path'])
                getTreeJson(homeUrl,newPaths,outputJsonArray)
    print("FOLDER ",dirToVisit," FINISHED")
    print("NUMBER OF FILES FOUND:",files)
    print("NUMBER OF FOLDERS FOUND:",subFolders)
    print("--------------------------------------------")
    return json.dumps(outputJsonArray,indent=4)

def getDirsHashesJson(homeUrl,dirsToSearch,outputJsonStruct):
    outputJsonStruct['hashes'] = []
    jsonString = requests.get(homeUrl, auth=(user,password)).content.decode()
    print("--------------------------------------------")
    jsonArray = json.loads(jsonString)
    print("--------------------------------------------")
    print(homeUrl)
    for dirName in dirsToSearch:
        for obj in jsonArray:
            if dirName == obj['name']:
                jsonHashStruct = {}
                jsonHashStruct['name'] = obj['name']
                jsonHashStruct['hash'] = obj['sha']
                outputJsonStruct['hashes'].append(jsonHashStruct)
    return json.dumps(outputJsonStruct,indent=4)



ogg = {}
dirs = ['mods','config','forge']
dirs2 = ['/forgiacraft2']
blacklist = ['forgiacraft2/config/agricraft/json','forgiacraft2/config/hats']
getDirsHashesJson(urlBase + '/forgiacraft2/',dirs,ogg)
print("JSON PARSER")
print("NUMBER OF REQUEST REMAINING: ",getNumberOfRequest())
ogg['files'] = []
jsonOut = getTreeJson(urlBase,dirs2,ogg)
print("SCAN COMPLETED\nHERE'S THE JSON:\n",jsonOut)
f = open('out.json','w+')
f.write(jsonOut)
f.close()
