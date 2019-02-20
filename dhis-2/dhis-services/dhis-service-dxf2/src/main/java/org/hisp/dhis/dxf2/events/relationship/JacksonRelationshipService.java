package org.hisp.dhis.dxf2.events.relationship;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.Relationships;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.hibernate.objectmapper.EmptyStringToNullStdDeserializer;
import org.hisp.dhis.hibernate.objectmapper.ParseDateStdDeserializer;
import org.hisp.dhis.hibernate.objectmapper.WriteDateStdSerializer;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service( "org.hisp.dhis.dxf2.events.relationship.RelationshipService" )
@Scope( value = "prototype", proxyMode = ScopedProxyMode.INTERFACES )
@Transactional
public class JacksonRelationshipService
    extends AbstractRelationshipService
{

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private RelationshipService relationshipService;

    private final static ObjectMapper XML_MAPPER = new XmlMapper();

    private final static ObjectMapper JSON_MAPPER = new ObjectMapper();

    static
    {
        SimpleModule module = new SimpleModule();
        module.addDeserializer( String.class, new EmptyStringToNullStdDeserializer() );
        module.addDeserializer( Date.class, new ParseDateStdDeserializer() );
        module.addSerializer( Date.class, new WriteDateStdSerializer() );

        XML_MAPPER.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        XML_MAPPER.configure( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true );
        XML_MAPPER.configure( DeserializationFeature.WRAP_EXCEPTIONS, true );
        JSON_MAPPER.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        JSON_MAPPER.configure( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true );
        JSON_MAPPER.configure( DeserializationFeature.WRAP_EXCEPTIONS, true );

        XML_MAPPER.disable( MapperFeature.AUTO_DETECT_FIELDS );
        XML_MAPPER.disable( MapperFeature.AUTO_DETECT_CREATORS );
        XML_MAPPER.disable( MapperFeature.AUTO_DETECT_GETTERS );
        XML_MAPPER.disable( MapperFeature.AUTO_DETECT_SETTERS );
        XML_MAPPER.disable( MapperFeature.AUTO_DETECT_IS_GETTERS );

        JSON_MAPPER.disable( MapperFeature.AUTO_DETECT_FIELDS );
        JSON_MAPPER.disable( MapperFeature.AUTO_DETECT_CREATORS );
        JSON_MAPPER.disable( MapperFeature.AUTO_DETECT_GETTERS );
        JSON_MAPPER.disable( MapperFeature.AUTO_DETECT_SETTERS );
        JSON_MAPPER.disable( MapperFeature.AUTO_DETECT_IS_GETTERS );

        JSON_MAPPER.registerModule( module );
        XML_MAPPER.registerModule( module );
    }

    @Override
    public ImportSummaries addRelationshipsJson( InputStream inputStream, ImportOptions importOptions )
        throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );
        List<Relationship> relationships = new ArrayList<>();

        JsonNode root = JSON_MAPPER.readTree( input );

        if ( root.get( "relationships" ) != null ) {
            Relationships fromJson = fromJson( input, Relationships.class );
            relationships.addAll( fromJson.getRelationships() );
        }
        else {
            Relationship fromJson = fromJson( input, Relationship.class );
            relationships.add( fromJson );
        }

        return processRelationshipList( relationships, updateImportOptions( importOptions ) );
    }

    @Override
    public ImportSummaries addRelationshipsXml( InputStream inputStream, ImportOptions importOptions )
        throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );
        List<Relationship> relationships = new ArrayList<>();

        try {
            Relationships fromXml = fromXml( input, Relationships.class );
            relationships.addAll( fromXml.getRelationships() );
        }
        catch ( JsonMappingException ex ) {
            Relationship fromXml = fromXml( input, Relationship.class );
            relationships.add( fromXml );
        }

        return processRelationshipList( relationships, updateImportOptions( importOptions ) );
    }

    @Override
    public ImportSummary updateRelationshipJson( String id, InputStream inputStream, ImportOptions importOptions )
        throws IOException
    {
        Relationship relationship = fromJson( inputStream, Relationship.class );
        relationship.setRelationship( id );

        return updateRelationship( relationship, updateImportOptions( importOptions ) );
    }

    @Override
    public ImportSummary updateRelationshipXml( String id, InputStream inputStream, ImportOptions importOptions )
        throws IOException
    {
        Relationship relationship = fromXml( inputStream, Relationship.class );
        relationship.setRelationship( id );

        return updateRelationship( relationship, updateImportOptions( importOptions ) );
    }

    protected ImportOptions updateImportOptions( ImportOptions importOptions )
    {
        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        if ( importOptions.getUser() == null )
        {
            importOptions.setUser( currentUserService.getCurrentUser() );
        }

        return importOptions;
    }

    @SuppressWarnings( "unchecked" )
    private static <T> T fromXml( InputStream inputStream, Class<?> clazz )
        throws IOException
    {
        return (T) XML_MAPPER.readValue( inputStream, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private static <T> T fromXml( String input, Class<?> clazz )
        throws IOException
    {
        return (T) XML_MAPPER.readValue( input, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private static <T> T fromJson( InputStream inputStream, Class<?> clazz )
        throws IOException
    {
        return (T) JSON_MAPPER.readValue( inputStream, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private static <T> T fromJson( String input, Class<?> clazz )
        throws IOException
    {
        return (T) JSON_MAPPER.readValue( input, clazz );
    }
}
