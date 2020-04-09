import jenkins.model.*
import hudson.security.*

def env = System.getenv()

def jenkins = Jenkins.getInstance()
if(!(jenkins.getSecurityRealm() instanceof HudsonPrivateSecurityRealm))
    jenkins.setSecurityRealm(new HudsonPrivateSecurityRealm(false))
