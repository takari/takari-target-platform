package io.takari.maven.targetplatform.plugins;

import de.pdark.decentxml.Document;

public interface PomProcessor {

  void process(Document document);

}
