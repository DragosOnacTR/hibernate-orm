/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.bytecode.enhance.plugins
 
import org.hibernate.bytecode.enhance.spi.Enhancer
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javax.persistence.Transient
import javax.persistence.Entity
import java.io.FileInputStream
import java.io.FileOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.Convention 
import org.gradle.api.tasks.TaskAction
import org.gradle.api.file.FileTree

/**
* Plugin to enhance Entities using the context(s) to determine
* which to apply.
* @author Jeremy Whiting
*/
public class EnhanceTask extends DefaultTask {
   
    public EnhanceTask() {
        super()
        setDescription('Enhances Entity classes for efficient association referencing.')
    }
   
    @TaskAction
    def enhance () {
        logger.info( 'enhance task started')
        ext.pool = new ClassPool(false)
        EnhancePluginConvention convention = (EnhancePluginConvention)project.getConvention().getPlugins().get( EnhancePlugin.PLUGIN_CONVENTION_NAME)
        convention.contexts.each(
        { String context ->
            ext.enhancer = new Enhancer(EnhanceTask.class.getClassLoader().loadClass(context).newInstance())
            FileTree tree = project.fileTree(dir: project.sourceSets.main.output.classesDir)
            tree.include '**/*.class'
            tree.each(
            { File file ->
                final byte[] enhancedBytecode;
                InputStream is = null;
                CtClass clas = null;
                try {
                    is = new FileInputStream(file.toString())
                    clas =ext.pool.makeClass(is)
                    if (!clas.hasAnnotation(Entity.class)){
                        logger.debug("Class $file not an annotated Entity class. skipping...")
                    } else {
                         enhancedBytecode = ext.enhancer.enhance( clas.getName(), clas.toBytecode() );
                    }
                }
                catch (Exception e) {
                    logger.error( "Unable to enhance class [${file.toString()}]", e )
                    return
                } finally {
                    try {
                       if (null != is) is.close();
                    } finally{}
                }
                if (null != enhancedBytecode){
                    if ( file.delete() ) {
                        if ( ! file.createNewFile() ) {
                           logger.error( "Unable to recreate class file [" + clas.getName() + "]")
                        }
                    }
                    else {
                       logger.error( "Unable to delete class file [" + clas.getName() + "]")
                    }
                    FileOutputStream outputStream = new FileOutputStream( file, false )
                    try {
                       outputStream.write( enhancedBytecode )
                       outputStream.flush()
                    }
                    finally {
                       try {
                          if (outputStream != null) outputStream.close()
                          clas.detach()//release memory
                       }
                       catch ( IOException ignore) {
                       }
                    }
                 } 
             }) 
         }) 
         logger.info( 'enhance task finished')
    }
} 
