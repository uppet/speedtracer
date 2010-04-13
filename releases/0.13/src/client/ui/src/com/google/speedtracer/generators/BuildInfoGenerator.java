/*
 * Copyright 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.speedtracer.generators;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.speedtracer.client.BuildInfo;

import java.io.PrintWriter;

/**
 * A generator that uses system properties to populate a client instance of
 * {@link BuildInfo}.
 */
public class BuildInfoGenerator extends Generator {

  private static final String REVISION_PROPERTY_NAME = "speedtracer.revision";

  private static final String BUILD_INFO_QN = BuildInfo.class.getCanonicalName();

  private static final String REVISION_METHOD_NAME = "getBuildRevision";

  private static final String TIME_METHOD_NAME = "getBuildTime";

  private static int getBuildRevision() {
    final String revision = System.getProperty(REVISION_PROPERTY_NAME);
    if (revision == null) {
      return 0;
    }
    return Integer.parseInt(revision);
  }

  @Override
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {

    if (!typeName.equals(BUILD_INFO_QN)) {
      logger.log(TreeLogger.ERROR, "The type passed to GWT.create() must be "
          + BUILD_INFO_QN);
      throw new UnableToCompleteException();
    }

    final TypeOracle typeOracle = context.getTypeOracle();
    JClassType typeClass;
    try {
      typeClass = typeOracle.getType(typeName);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "Unable to find type: " + typeName);
      throw new UnableToCompleteException();
    }

    JMethod revisionMethod;
    try {
      revisionMethod = typeClass.getMethod("getBuildRevision", new JType[0]);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, typeName + " does not have a method: int "
          + REVISION_METHOD_NAME + "()");
      throw new UnableToCompleteException();
    }

    JMethod timeMethod;
    try {
      timeMethod = typeClass.getMethod(TIME_METHOD_NAME, new JType[0]);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, typeName + " does not have a method: int "
          + TIME_METHOD_NAME + "()");
      throw new UnableToCompleteException();
    }

    return emitImplClass(logger, context, typeClass, revisionMethod, timeMethod);
  }

  private String emitImplClass(TreeLogger logger, GeneratorContext context,
      JClassType typeClass, JMethod revisionMethod, JMethod timeMethod) {
    final int revision = getBuildRevision();
    final String subclassName = typeClass.getSimpleSourceName() + "_r"
        + revision;
    final String packageName = typeClass.getPackage().getName();
    final ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
        packageName, subclassName);
    f.addImplementedInterface(typeClass.getQualifiedSourceName());
    final PrintWriter pw = context.tryCreate(logger, packageName, subclassName);
    if (pw != null) {
      final SourceWriter sw = f.createSourceWriter(context, pw);

      sw.print(revisionMethod.getReadableDeclaration(false, true, true, true,
          true));
      sw.println("{ return " + revision + "; }");

      sw.print(timeMethod.getReadableDeclaration(false, true, true, true, true));
      sw.println("{ return new java.util.Date(" + System.currentTimeMillis()
          + "L); }");
      sw.commit(logger);
    }
    return f.getCreatedClassName();
  }
}
