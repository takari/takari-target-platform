package io.takari.maven.targetplatform;

import javax.inject.Named;

import org.apache.maven.classrealm.ClassRealmRequest;
import org.apache.maven.classrealm.ClassRealmRequest.RealmType;
import org.codehaus.plexus.classworlds.realm.ClassRealm;


// TODO META-INF/maven/extension.xml should work for core extensions too

@Named
public class ClassRealmManagerDelegate implements
    org.apache.maven.classrealm.ClassRealmManagerDelegate {

  @Override
  public void setupRealm(ClassRealm classRealm, ClassRealmRequest request) {
    if (request.getType() == RealmType.Extension || request.getType() == RealmType.Plugin) {
      classRealm.importFrom(getClass().getClassLoader(), "io.takari.maven.targetplatform");
      // Iterator<ClassRealmConstituent> iterator = request.getConstituents().iterator();
      // while (iterator.hasNext()) {
      // ClassRealmConstituent member = iterator.next();
      // if ("io.takari.maven".equals(member.getGroupId())
      // && "takari-targetplatform".equals(member.getArtifactId())) {
      // iterator.remove();
      // classRealm.importFrom(getClass().getClassLoader(), "io.takari.maven.targetplatform");
      // }
      // }
    }
  }

}
