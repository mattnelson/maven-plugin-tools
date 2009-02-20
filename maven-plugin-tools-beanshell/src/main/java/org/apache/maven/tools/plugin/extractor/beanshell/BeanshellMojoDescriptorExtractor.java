package org.apache.maven.tools.plugin.extractor.beanshell;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.AbstractScriptedMojoDescriptorExtractor;
import org.apache.maven.tools.plugin.extractor.ExtractionException;

import java.io.File;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * Extracts Mojo descriptors from <a href="http://www.beanshell.org/">BeanShell</a> sources.
 *
 * @todo share constants
 * @todo add example usage tag that can be shown in the doco
 * @todo need to add validation directives so that systems embedding maven2 can
 * get validation directives to help users in IDEs.
 * @version $Id$
 */
public class BeanshellMojoDescriptorExtractor
    extends AbstractScriptedMojoDescriptorExtractor
{
    /** {@inheritDoc} */
    protected String getScriptFileExtension( PluginToolsRequest request )
    {
        return ".bsh";
    }

    /** {@inheritDoc} */
    protected List extractMojoDescriptors( Map scriptFilesKeyedByBasedir, PluginToolsRequest request )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        List descriptors = new ArrayList();

        for ( Iterator mapIterator = scriptFilesKeyedByBasedir.entrySet().iterator(); mapIterator.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) mapIterator.next();

            String basedir = (String) entry.getKey();
            Set metadataFiles = (Set) entry.getValue();

            for ( Iterator it = metadataFiles.iterator(); it.hasNext(); )
            {
                File scriptFile = (File) it.next();

                String relativePath = null;

                if ( basedir.endsWith( "/" ) )
                {
                    basedir = basedir.substring( 0, basedir.length() - 2 );
                }

                relativePath = scriptFile.getPath().substring( basedir.length() );

                relativePath = relativePath.replace( '\\', '/' );

                MojoDescriptor mojoDescriptor = createMojoDescriptor( basedir, relativePath, request );
                descriptors.add( mojoDescriptor );
            }
        }

        return descriptors;
    }

    /**
     * @param basedir not null
     * @param resource not null
     * @param pluginDescriptor not null
     * @return a new Mojo descriptor instance
     * @throws InvalidPluginDescriptorException if any
     */
    private MojoDescriptor createMojoDescriptor( String basedir, String resource, PluginToolsRequest request )
        throws InvalidPluginDescriptorException
    {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPluginDescriptor( request.getPluginDescriptor() );

        mojoDescriptor.setLanguage( "bsh" );
        mojoDescriptor.setComponentConfigurator( "bsh" );

        mojoDescriptor.setImplementation( resource );

        Interpreter interpreter = new Interpreter();

        try
        {
            interpreter.set( "file", new File( basedir, resource ) );

            interpreter.set( "mojoDescriptor", mojoDescriptor );

            interpreter.eval( new InputStreamReader( getClass().getResourceAsStream( "/extractor.bsh" ), request.getEncoding() ) );
        }
        catch ( EvalError evalError )
        {
            throw new InvalidPluginDescriptorException( "Error scanning beanshell script", evalError );
        }
        catch ( UnsupportedEncodingException uee )
        {
            // should not occur...
            throw new InvalidPluginDescriptorException( "Unsupported encoding while reading beanshell script", uee );
        }

        return mojoDescriptor;
    }
}