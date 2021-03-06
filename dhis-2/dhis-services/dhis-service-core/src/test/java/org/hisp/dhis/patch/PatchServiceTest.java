package org.hisp.dhis.patch;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.hibernate.objectmapper.EmptyStringToNullStdDeserializer;
import org.hisp.dhis.hibernate.objectmapper.ParseDateStdDeserializer;
import org.hisp.dhis.hibernate.objectmapper.WriteDateStdSerializer;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupAccess;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class PatchServiceTest
    extends DhisSpringTest
{
    @Autowired
    private PatchService patchService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserService _userService;

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    static
    {
        SimpleModule module = new SimpleModule();
        module.addDeserializer( String.class, new EmptyStringToNullStdDeserializer() );
        module.addDeserializer( Date.class, new ParseDateStdDeserializer() );
        module.addSerializer( Date.class, new WriteDateStdSerializer() );

        jsonMapper.registerModule( module );

        jsonMapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
        jsonMapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        jsonMapper.disable( SerializationFeature.WRITE_EMPTY_JSON_ARRAYS );
        jsonMapper.disable( SerializationFeature.FAIL_ON_EMPTY_BEANS );
        jsonMapper.enable( SerializationFeature.WRAP_EXCEPTIONS );

        jsonMapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        jsonMapper.enable( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES );
        jsonMapper.enable( DeserializationFeature.WRAP_EXCEPTIONS );

        jsonMapper.disable( MapperFeature.AUTO_DETECT_FIELDS );
        jsonMapper.disable( MapperFeature.AUTO_DETECT_CREATORS );
        jsonMapper.disable( MapperFeature.AUTO_DETECT_GETTERS );
        jsonMapper.disable( MapperFeature.AUTO_DETECT_SETTERS );
        jsonMapper.disable( MapperFeature.AUTO_DETECT_IS_GETTERS );

        jsonMapper.getFactory().enable( JsonGenerator.Feature.QUOTE_FIELD_NAMES );
    }

    @Override
    protected void setUpTest() throws Exception
    {
        userService = _userService;
    }

    @Test
    public void testUpdateName()
    {
        DataElement dataElement = createDataElement( 'A' );

        Patch patch = new Patch()
            .addMutation( new Mutation( "name", "Updated Name" ) );

        patchService.apply( patch, dataElement );

        assertEquals( "Updated Name", dataElement.getName() );
    }

    @Test
    public void testAddDataElementToGroup()
    {
        DataElementGroup dataElementGroup = createDataElementGroup( 'A' );
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );

        manager.save( deA );
        manager.save( deB );

        assertTrue( dataElementGroup.getMembers().isEmpty() );

        Patch patch = new Patch()
            .addMutation( new Mutation( "name", "Updated Name" ) )
            .addMutation( new Mutation( "dataElements", Lists.newArrayList( deA.getUid(), deB.getUid() ) ) );

        patchService.apply( patch, dataElementGroup );

        assertEquals( "Updated Name", dataElementGroup.getName() );
        assertEquals( 2, dataElementGroup.getMembers().size() );
    }

    @Test
    public void testDeleteDataElementFromGroup()
    {
        DataElementGroup dataElementGroup = createDataElementGroup( 'A' );
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );

        manager.save( deA );
        manager.save( deB );

        dataElementGroup.addDataElement( deA );
        dataElementGroup.addDataElement( deB );

        assertEquals( 2, dataElementGroup.getMembers().size() );

        Patch patch = new Patch()
            .addMutation( new Mutation( "name", "Updated Name" ) )
            .addMutation( new Mutation( "dataElements", Lists.newArrayList( deA.getUid() ), Mutation.Operation.DELETION ) );

        patchService.apply( patch, dataElementGroup );

        assertEquals( "Updated Name", dataElementGroup.getName() );
        assertEquals( 1, dataElementGroup.getMembers().size() );

        patch = new Patch()
            .addMutation( new Mutation( "dataElements", Lists.newArrayList( deB.getUid() ), Mutation.Operation.DELETION ) );

        patchService.apply( patch, dataElementGroup );

        assertTrue( dataElementGroup.getMembers().isEmpty() );
    }

    @Test
    public void testAddAggLevelsToDataElement()
    {
        DataElement dataElement = createDataElement( 'A' );
        assertTrue( dataElement.getAggregationLevels().isEmpty() );

        Patch patch = new Patch()
            .addMutation( new Mutation( "name", "Updated Name" ) )
            .addMutation( new Mutation( "aggregationLevels", 1 ) )
            .addMutation( new Mutation( "aggregationLevels", 2 ) );

        patchService.apply( patch, dataElement );

        assertEquals( 2, dataElement.getAggregationLevels().size() );
    }

    @Test
    public void testUpdateValueTypeEnumFromString()
    {
        DataElement dataElement = createDataElement( 'A' );
        assertTrue( dataElement.getAggregationLevels().isEmpty() );

        Patch patch = new Patch()
            .addMutation( new Mutation( "name", "Updated Name" ) )
            .addMutation( new Mutation( "domainType", "TRACKER" ) )
            .addMutation( new Mutation( "valueType", "BOOLEAN" ) );

        patchService.apply( patch, dataElement );

        assertEquals( DataElementDomain.TRACKER, dataElement.getDomainType() );
        assertEquals( ValueType.BOOLEAN, dataElement.getValueType() );
    }

    @Test
    public void testUpdateUserOnDataElement()
    {
        User user = createUser( 'A' );
        manager.save( user );

        createAndInjectAdminUser();

        DataElement dataElement = createDataElement( 'A' );
        manager.save( dataElement );

        Patch patch = new Patch()
            .addMutation( new Mutation( "name", "Updated Name" ) )
            .addMutation( new Mutation( "user", user.getUid() ) )
            .addMutation( new Mutation( "domainType", "TRACKER" ) )
            .addMutation( new Mutation( "valueType", "BOOLEAN" ) );

        patchService.apply( patch, dataElement );

        assertEquals( DataElementDomain.TRACKER, dataElement.getDomainType() );
        assertEquals( ValueType.BOOLEAN, dataElement.getValueType() );
        assertEquals( user.getUid(), dataElement.getUser().getUid() );
    }

    @Test
    public void testAddStringAggLevelsToDataElement()
    {
        DataElement dataElement = createDataElement( 'A' );
        assertTrue( dataElement.getAggregationLevels().isEmpty() );

        Patch patch = new Patch()
            .addMutation( new Mutation( "name", "Updated Name" ) )
            .addMutation( new Mutation( "aggregationLevels", "1" ) )
            .addMutation( new Mutation( "aggregationLevels", "abc" ) )
            .addMutation( new Mutation( "aggregationLevels", "def" ) );

        patchService.apply( patch, dataElement );
        assertTrue( dataElement.getAggregationLevels().isEmpty() );
    }

    @Test
    public void testUpdateUserCredentialsOnUser()
    {
        User user = createAndInjectAdminUser();
        assertEquals( "admin", user.getUserCredentials().getUsername() );

        Patch patch = new Patch()
            .addMutation( new Mutation( "userCredentials.username", "dhis" ) );

        patchService.apply( patch, user );

        assertEquals( "dhis", user.getUserCredentials().getUsername() );
    }

    @Test
    public void testSimpleDiff()
    {
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );

        Patch patch = patchService.diff( new PatchParams( deA, deB ) );
        patchService.apply( patch, deA );

        assertEquals( deA.getName(), deB.getName() );
        assertEquals( deA.getShortName(), deB.getShortName() );
        assertEquals( deA.getDescription(), deB.getDescription() );
    }

    @Test
    public void testSimpleCollectionDiff()
    {
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );

        deA.getAggregationLevels().add( 1 );
        deB.getAggregationLevels().add( 2 );
        deB.getAggregationLevels().add( 3 );

        Patch patch = patchService.diff( new PatchParams( deA, deB ) );

        checkCount( patch, "aggregationLevels", Mutation.Operation.ADDITION, 2 );
        checkCount( patch, "aggregationLevels", Mutation.Operation.DELETION, 1 );

        patchService.apply( patch, deA );

        assertEquals( deA.getName(), deB.getName() );
        assertEquals( deA.getShortName(), deB.getShortName() );
        assertEquals( deA.getDescription(), deB.getDescription() );
        assertEquals( deA.getAggregationLevels(), deB.getAggregationLevels() );
    }

    @Test
    public void testSimpleIdObjectCollectionDiff()
    {
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );

        DataElementGroup degA = createDataElementGroup( 'C' );
        DataElementGroup degB = createDataElementGroup( 'D' );

        manager.save( degA );
        manager.save( degB );

        deA.getGroups().add( degA );
        manager.update( degA );

        deB.getGroups().add( degB );

        deA.getAggregationLevels().add( 1 );
        deA.getAggregationLevels().add( 2 );

        deB.getAggregationLevels().add( 2 );
        deB.getAggregationLevels().add( 3 );
        deB.getAggregationLevels().add( 4 );

        Patch patch = patchService.diff( new PatchParams( deA, deB ) );

        checkCount( patch, "dataElementGroups", Mutation.Operation.ADDITION, 1 );
        checkCount( patch, "dataElementGroups", Mutation.Operation.DELETION, 1 );

        checkCount( patch, "aggregationLevels", Mutation.Operation.ADDITION, 2 );
        checkCount( patch, "aggregationLevels", Mutation.Operation.DELETION, 1 );

        patchService.apply( patch, deA );

        assertEquals( deA.getName(), deB.getName() );
        assertEquals( deA.getShortName(), deB.getShortName() );
        assertEquals( deA.getDescription(), deB.getDescription() );
        assertEquals( deA.getAggregationLevels(), deB.getAggregationLevels() );
        assertEquals( deA.getGroups(), deB.getGroups() );
    }

    @Test
    public void testEmbeddedObjectEquality()
    {
        User adminUser = createAndInjectAdminUser();
        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( adminUser ) );
        manager.save( userGroup );

        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );

        deA.getUserGroupAccesses().add( new UserGroupAccess( userGroup, "rw------" ) );
        deA.getUserAccesses().add( new UserAccess( adminUser, "rw------" ) );

        deB.getUserGroupAccesses().add( new UserGroupAccess( userGroup, "rw------" ) );
        deB.getUserAccesses().add( new UserAccess( adminUser, "rw------" ) );

        patchService.diff( new PatchParams( deA, deB ) );
    }

    @Test
    public void testEmbeddedObjectCollectionDiff()
    {
        User adminUser = createAndInjectAdminUser();
        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( adminUser ) );
        manager.save( userGroup );

        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );

        deA.getAggregationLevels().add( 1 );
        deB.getAggregationLevels().add( 1 );
        deB.getAggregationLevels().add( 2 );
        deB.getAggregationLevels().add( 3 );

        deB.getUserGroupAccesses().add( new UserGroupAccess( userGroup, "rw------" ) );
        deB.getUserAccesses().add( new UserAccess( adminUser, "rw------" ) );

        Patch patch = patchService.diff( new PatchParams( deA, deB ) );
        patchService.apply( patch, deA );

        assertEquals( deA.getName(), deB.getName() );
        assertEquals( deA.getShortName(), deB.getShortName() );
        assertEquals( deA.getDescription(), deB.getDescription() );
        assertEquals( deA.getAggregationLevels(), deB.getAggregationLevels() );
        assertEquals( deA.getUserGroupAccesses(), deB.getUserGroupAccesses() );
        assertEquals( deA.getUserAccesses(), deB.getUserAccesses() );
    }

    @Test
    public void testPatchFromJsonNode1()
    {
        JsonNode jsonNode = loadJsonNodeFromFile( "patch/simple.json" );
        DataElement dataElement = createDataElement( 'A' );

        Patch patch = patchService.diff( new PatchParams( jsonNode ) );
        assertEquals( 2, patch.getMutations().size() );

        patchService.apply( patch, dataElement );

        assertEquals( dataElement.getName(), "Updated Name" );
        assertEquals( dataElement.getShortName(), "Updated Short Name" );
    }

    @Test
    public void testPatchFromJsonNode2()
    {
        JsonNode jsonNode = loadJsonNodeFromFile( "patch/id-collection.json" );
        DataElement dataElement = createDataElement( 'A' );

        DataElementGroup degA = createDataElementGroup( 'C' );
        DataElementGroup degB = createDataElementGroup( 'D' );

        manager.save( degA );
        manager.save( degB );

        Patch patch = patchService.diff( new PatchParams( jsonNode ) );
        patchService.apply( patch, dataElement );

        assertEquals( dataElement.getName(), "Updated Name" );
        assertEquals( dataElement.getShortName(), "Updated Short Name" );
        assertEquals( 2, dataElement.getGroups().size() );
    }

    @Test
    public void testPatchFromJsonNode3()
    {
        JsonNode jsonNode = loadJsonNodeFromFile( "patch/complex.json" );
        patchService.diff( new PatchParams( jsonNode ) );
    }

    private JsonNode loadJsonNodeFromFile( String path )
    {
        try
        {
            InputStream inputStream = new ClassPathResource( path ).getInputStream();
            return jsonMapper.readTree( inputStream );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }

        return null;
    }

    private void checkCount( Patch patch, String name, Mutation.Operation operation, int expected )
    {
        int count = 0;

        for ( Mutation mutation : patch.getMutations() )
        {
            if ( mutation.getOperation() == operation && mutation.getPath().equals( name ) )
            {
                if ( Collection.class.isInstance( mutation.getValue() ) )
                {
                    count += ((Collection<?>) mutation.getValue()).size();
                }
                else
                {
                    count++;
                }
            }
        }

        assertEquals( "Did not find " + expected + " mutations of type " + operation + " on property " + name, expected, count );
    }
}
