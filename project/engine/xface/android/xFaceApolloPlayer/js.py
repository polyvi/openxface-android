import os
import subprocess
import sys
#run command and return the status code
GENERATEFILENAME = "GenerateXFaceJs.py"
PLATFORM = "android"
def runCommand(command_str):
    output = subprocess.Popen(command_str, shell= True , stdout=subprocess.PIPE)
    while True:
        str = output.stdout.read()
        if not len(str): break
        print str
    output.wait()
    return output.returncode

def generateXFaceJs():
    commonFilePath = os.path.normpath(os.path.join(os.path.dirname(os.path.realpath(__file__)),r"../../../xface/js/generater",GENERATEFILENAME))
    print commonFilePath
    destFilePath = os.path.join(os.path.dirname(os.path.realpath(__file__)),"res/raw/xface.js")
    appsDir =  os.path.join(os.path.dirname(os.path.realpath(__file__)),"assets/data")
    runCommand("python " + commonFilePath + " " + destFilePath + " " + appsDir + " " + PLATFORM)
if __name__=="__main__":
    generateXFaceJs()
