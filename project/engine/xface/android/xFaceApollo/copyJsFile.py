import os
import subprocess
import sys
#run command and return the status code
COPYFILENAME = "copyJsFile.py"
def runCommand(command_str):
    output = subprocess.Popen(command_str, shell= True , stdout=subprocess.PIPE)
    while True:
        str = output.stdout.read()
        if not len(str): break
        print str
    output.wait()
    return output.returncode

def copyXFaceJsToAppsDir():
    commonFilePath = os.path.normpath(os.path.join(os.path.dirname(os.path.realpath(__file__)),r"../../../xface/js/xface_js_generater",COPYFILENAME))
    destFilePath = os.path.join(os.path.dirname(os.path.realpath(__file__)),"res/raw/xface.js")
    appsDir =  os.path.join(os.path.dirname(os.path.realpath(__file__)),"assets/data")
    runCommand("python " + commonFilePath + " " + destFilePath + " " + appsDir)
if __name__=="__main__":
    copyXFaceJsToAppsDir()