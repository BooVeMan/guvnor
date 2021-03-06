/*
 * Copyright 2011 JBoss Inc
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.drools.guvnor.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.drools.guvnor.client.common.AssetFormats;
import org.drools.guvnor.client.explorer.ExplorerNodeConfig;
import org.drools.guvnor.client.rpc.AdminArchivedPageRow;
import org.drools.guvnor.client.rpc.BuilderResult;
import org.drools.guvnor.client.rpc.PageRequest;
import org.drools.guvnor.client.rpc.PageResponse;
import org.drools.guvnor.client.rpc.RepositoryService;
import org.drools.guvnor.client.rpc.RuleAsset;
import org.drools.guvnor.client.rpc.RuleContentText;
import org.drools.guvnor.client.rpc.TableDataResult;
import org.drools.guvnor.client.rpc.TableDataRow;
import org.drools.guvnor.server.cache.RuleBaseCache;
import org.drools.guvnor.server.util.DroolsHeader;
import org.drools.ide.common.client.modeldriven.SuggestionCompletionEngine;
import org.drools.ide.common.client.modeldriven.brl.ActionFieldValue;
import org.drools.ide.common.client.modeldriven.brl.ActionSetField;
import org.drools.ide.common.client.modeldriven.brl.FactPattern;
import org.drools.ide.common.client.modeldriven.brl.RuleModel;
import org.drools.ide.common.client.modeldriven.brl.SingleFieldConstraint;
import org.drools.repository.AssetItem;
import org.drools.repository.AssetItemIterator;
import org.drools.repository.PackageItem;
import org.drools.repository.RulesRepository;
import org.drools.repository.RulesRepositoryException;
import org.junit.Ignore;
import org.junit.Test;

public class RepositoyAssetServiceTest extends GuvnorTestBase {
    @Test
    public void testCreateLinkedAssetItem() throws Exception {
        ServiceImplementation impl = getServiceImplementation();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        @SuppressWarnings("unused")
        PackageItem testCreateNewRuleAsLinkPackage1 = impl.getRulesRepository().createPackage( "testCreateNewRuleAsLinkPackage1",
                                                                                               "desc" );
        repositoryCategoryService.createCategory( "",
                                                  "testCreateNewRuleAsLinkCat1",
                                                  "this is a cat" );
        repositoryCategoryService.createCategory( "",
                                                  "testCreateNewRuleAsLinkCat2",
                                                  "this is a cat" );

        //Create the shared asset.
        String uuid = impl.createNewRule( "testCreateLinkedAssetItemRule",
                                          "an initial desc",
                                          "testCreateNewRuleAsLinkCat1",
                                          "globalArea",
                                          AssetFormats.DSL_TEMPLATE_RULE );
        assertNotNull( uuid );
        assertFalse( "".equals( uuid ) );

        AssetItem dtItem = impl.getRulesRepository().loadAssetByUUID( uuid );
        assertEquals( dtItem.getDescription(),
                      "an initial desc" );

        //create an asset which is imported from global area. 
        String uuidLink = impl.createNewImportedRule( "testCreateLinkedAssetItemRule",
                                                      "testCreateNewRuleAsLinkPackage1" );
        assertNotNull( uuidLink );
        assertFalse( "".equals( uuidLink ) );
        assertTrue( uuidLink.equals( uuid ) );

        //now verify the linked asset.
        AssetItem itemLink = impl.getRulesRepository().loadAssetByUUID( uuidLink );
        assertEquals( itemLink.getName(),
                      "testCreateLinkedAssetItemRule" );
        assertEquals( itemLink.getDescription(),
                      "an initial desc" );
        assertEquals( itemLink.getFormat(),
                      AssetFormats.DSL_TEMPLATE_RULE );
        assertEquals( itemLink.getPackage().getName(),
                      "globalArea" );

        assertEquals( itemLink.getPackageName(),
                      "globalArea" );

        assertTrue( itemLink.getCategories().size() == 1 );
        assertTrue( itemLink.getCategorySummary().contains( "testCreateNewRuleAsLinkCat1" ) );

        //now verify the original asset.
        AssetItem referredItem = impl.getRulesRepository().loadAssetByUUID( uuid );
        assertEquals( referredItem.getName(),
                      "testCreateLinkedAssetItemRule" );
        assertEquals( referredItem.getDescription(),
                      "an initial desc" );
        assertEquals( referredItem.getFormat(),
                      AssetFormats.DSL_TEMPLATE_RULE );
        assertEquals( referredItem.getPackage().getName(),
                      "globalArea" );

        assertTrue( referredItem.getCategories().size() == 1 );
        assertTrue( referredItem.getCategorySummary().contains( "testCreateNewRuleAsLinkCat1" ) );

        //now verify AssetItemIterator works by calling search
        AssetItemIterator it = impl.getRulesRepository().findAssetsByName( "testCreateLinkedAssetItemRule%",
                                                                           true );
        //NOTE, getSize() may return -1
        /*       assertEquals( 1,
                             it.getSize() );*/
        int size = 0;
        while ( it.hasNext() ) {
            size++;
            AssetItem ai = it.next();
            if ( ai.getUUID().equals( uuid ) ) {
                assertEquals( ai.getPackage().getName(),
                              "globalArea" );
                assertEquals( ai.getDescription(),
                              "an initial desc" );
            } else {
                fail( "unexptected asset found: " + ai.getPackage().getName() );
            }
        }
        assertEquals( 1,
                      size );
    }

    @Test
    @Ignore("This test is broken. Suspect changes for new Workspaces and versionable packages could have broken.")
    public void testLinkedAssetItemHistoryRelated() throws Exception {
        ServiceImplementation impl = getServiceImplementation();
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        @SuppressWarnings("unused")
        PackageItem testCreateNewRuleAsLinkPackage1 = impl.getRulesRepository().createPackage( "testLinkedAssetItemHistoryRelatedPack",
                                                                                               "desc" );
        repositoryCategoryService.createCategory( "",
                                                  "testLinkedAssetItemHistoryRelatedCat",
                                                  "this is a cat" );

        //Create the shared asset in global area.
        String uuid = impl.createNewRule( "testLinkedAssetItemHistoryRelatedRule",
                                          "an initial desc",
                                          "testLinkedAssetItemHistoryRelatedCat",
                                          "globalArea",
                                          AssetFormats.DSL_TEMPLATE_RULE );

        //create an asset which refers to the shared assets.
        String uuidLink = impl.createNewImportedRule( "testLinkedAssetItemHistoryRelatedRule",
                                                      "testLinkedAssetItemHistoryRelatedPack" );
        assertTrue( uuidLink.equals( uuid ) );

        //create version 1.
        RuleAsset assetWrapper = repositoryAssetService.loadRuleAsset( uuidLink );
        assertEquals( assetWrapper.description,
                      "an initial desc" );
        assetWrapper.description="version 1" ;
        String uuidLink1 = repositoryAssetService.checkinVersion( assetWrapper );

        //create version 2
        RuleAsset assetWrapper2 = repositoryAssetService.loadRuleAsset( uuidLink );
        assetWrapper2.description= "version 2" ;
        String uuidLink2 = repositoryAssetService.checkinVersion( assetWrapper2 );

        //create version head
        RuleAsset assetWrapper3 = repositoryAssetService.loadRuleAsset( uuidLink );
        assetWrapper3.description= "version head" ;
        @SuppressWarnings("unused")
        String uuidLink3 = repositoryAssetService.checkinVersion( assetWrapper3 );

        assertEquals( uuidLink,
                      uuidLink1 );
        assertEquals( uuidLink,
                      uuidLink2 );

        //verify the history info of LinkedAssetItem
        TableDataResult result = repositoryAssetService.loadItemHistory( uuidLink );
        assertNotNull( result );
        TableDataRow[] rows = result.data;
        assertEquals( 3,
                      rows.length );
        assertFalse( rows[0].id.equals( uuidLink ) );
        assertFalse( rows[1].id.equals( uuidLink ) );
        assertFalse( rows[2].id.equals( uuidLink ) );

        RuleAsset version1 = repositoryAssetService.loadRuleAsset( rows[0].id );
        RuleAsset version2 = repositoryAssetService.loadRuleAsset( rows[1].id );
        RuleAsset version3 = repositoryAssetService.loadRuleAsset( rows[2].id );
        RuleAsset versionHead = repositoryAssetService.loadRuleAsset( uuidLink );
        assertFalse( version1.versionNumber== version2.versionNumber );
        assertFalse( version1.versionNumber == versionHead.versionNumber );
        assertEquals( version1.description,
                      "an initial desc" );
        assertEquals( version2.description,
                      "version 1" );
        assertEquals( version3.description,
                      "version 2" );
        assertEquals( versionHead.description,
                      "version head" );

        //verify the history info of the original AssetItem
        result = repositoryAssetService.loadItemHistory( uuid );
        assertNotNull( result );
        rows = result.data;
        assertEquals( 3,
                      rows.length );
        assertFalse( rows[0].id.equals( uuid ) );
        assertFalse( rows[1].id.equals( uuid ) );

        version1 = repositoryAssetService.loadRuleAsset( rows[0].id );
        version2 = repositoryAssetService.loadRuleAsset( rows[1].id );
        versionHead = repositoryAssetService.loadRuleAsset( uuid );
        assertFalse( version1.versionNumber == version2.versionNumber );
        assertFalse( version1.versionNumber == versionHead.versionNumber );
        assertTrue( version1.description.equals( "an initial desc" ) );
        assertTrue( version2.description.equals( "version 1" ) );
        assertTrue( versionHead.description.equals( "version head" ) );

        //test restore
        repositoryAssetService.restoreVersion( version1.uuid,
                                               versionHead.uuid,
                                               "this was cause of a mistake" );

        RuleAsset newHead = repositoryAssetService.loadRuleAsset( uuid );

        assertEquals( "this was cause of a mistake",
                      newHead.checkinComment );
    }

    @Test
    @Deprecated
    public void testLoadRuleAsset() throws Exception {
        ServiceImplementation impl = getServiceImplementation();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        impl.getRulesRepository().createPackage( "testLoadRuleAsset",
                                                 "desc" );
        repositoryCategoryService.createCategory( "",
                                                  "testLoadRuleAsset",
                                                  "this is a cat" );

        impl.createNewRule( "testLoadRuleAsset",
                            "description",
                            "testLoadRuleAsset",
                            "testLoadRuleAsset",
                            AssetFormats.DRL );

        TableDataResult res = repositoryCategoryService.loadRuleListForCategories( "testLoadRuleAsset",
                                                                                   0,
                                                                                   -1,
                                                                                   ExplorerNodeConfig.RULE_LIST_TABLE_ID );
        assertEquals( 1,
                      res.data.length );
        assertEquals( -1,
                      res.total );
        assertTrue( res.currentPosition > 0 );
        assertFalse( res.hasNext );

        TableDataRow row = res.data[0];
        String uuid = row.id;
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        RuleAsset asset = repositoryAssetService.loadRuleAsset( uuid );
        assertNotNull( asset );

        assertEquals( uuid,
                      asset.uuid );

        assertEquals( "description",
                      asset.description );

        assertNotNull( asset.content  );
        assertTrue( asset.content instanceof RuleContentText );
        assertEquals( "testLoadRuleAsset",
                      asset.name );
        assertEquals( "testLoadRuleAsset",
                      asset.metaData.title );
        assertEquals( "testLoadRuleAsset",
                      asset.metaData.packageName );
        assertEquals( AssetFormats.DRL,
                      asset.metaData.format );
        assertNotNull( asset.dateCreated );

        assertEquals( 1,
                      asset.metaData.categories.length );
        assertEquals( "testLoadRuleAsset",
                      asset.metaData.categories[0] );

        AssetItem rule = impl.getRulesRepository().loadPackage( "testLoadRuleAsset" ).loadAsset( "testLoadRuleAsset" );
        impl.getRulesRepository().createState( "whee" );
        rule.updateState( "whee" );
        rule.checkin( "changed state" );
        asset = repositoryAssetService.loadRuleAsset( uuid );

        assertEquals( "whee",
                      asset.state );
        assertEquals( "changed state",
                      asset.checkinComment );

        uuid = impl.createNewRule( "testBRLFormatSugComp",
                                   "description",
                                   "testLoadRuleAsset",
                                   "testLoadRuleAsset",
                                   AssetFormats.BUSINESS_RULE );
        asset = repositoryAssetService.loadRuleAsset( uuid );
        assertTrue( asset.content instanceof RuleModel );

        uuid = impl.createNewRule( "testLoadRuleAssetBRL",
                                   "description",
                                   "testLoadRuleAsset",
                                   "testLoadRuleAsset",
                                   AssetFormats.DSL_TEMPLATE_RULE );
        asset = repositoryAssetService.loadRuleAsset( uuid );
        assertTrue( asset.content instanceof RuleContentText );
    }

    @Test
    @Deprecated
    public void testListAssets() throws Exception {
        ServiceImplementation impl = getServiceImplementation();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        PackageItem pacakgeItem = impl.getRulesRepository().createPackage( "testListAssetsPackage",
                                                                           "desc" );
        repositoryCategoryService.createCategory( "",
                                                  "testListAssetsCat",
                                                  "this is a cat" );

        impl.createNewRule( "testLoadArchivedAssets1",
                                           "description",
                                           "testListAssetsCat",
                                           "testListAssetsPackage",
                                           AssetFormats.BUSINESS_RULE );

        impl.createNewRule( "testLoadArchivedAssets2",
                                           "description",
                                           "testListAssetsCat",
                                           "testListAssetsPackage",
                                           AssetFormats.BUSINESS_RULE );

        impl.createNewRule( "testLoadArchivedAssets3",
                                           "description",
                                           "testListAssetsCat",
                                           "testListAssetsPackage",
                                           AssetFormats.BUSINESS_RULE );

        impl.createNewRule( "testLoadArchivedAssets4",
                                           "description",
                                           "testListAssetsCat",
                                           "testListAssetsPackage",
                                           AssetFormats.BUSINESS_RULE );

        impl.createNewRule( "testLoadArchivedAssets5",
                                           "description",
                                           "testListAssetsCat",
                                           "testListAssetsPackage",
                                           AssetFormats.BUSINESS_RULE );
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        TableDataResult res = repositoryAssetService.listAssets( pacakgeItem.getUUID(),
                                                                 new String[]{AssetFormats.BUSINESS_RULE},
                                                                 0,
                                                                 2,
                                                                 ExplorerNodeConfig.PACKAGEVIEW_LIST_TABLE_ID );

        assertEquals( 2,
                      res.data.length );
        assertTrue( 5 == res.total );
        assertTrue( res.currentPosition == 2 );
        assertTrue( res.hasNext );

        res = repositoryAssetService.listAssets( pacakgeItem.getUUID(),
                                                 new String[]{AssetFormats.BUSINESS_RULE},
                                                 2,
                                                 2,
                                                 ExplorerNodeConfig.PACKAGEVIEW_LIST_TABLE_ID );
        assertEquals( 2,
                      res.data.length );
        assertTrue( 5 == res.total );
        assertTrue( res.currentPosition == 4 );
        assertTrue( res.hasNext );
    }

    @Test
    public void testLoadArchivedAssets() throws Exception {
        ServiceImplementation impl = getServiceImplementation();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        impl.getRulesRepository().createPackage( "testLoadArchivedAssetsPackage",
                                                 "desc" );
        repositoryCategoryService.createCategory( "",
                                                  "testLoadArchivedAssetsCat",
                                                  "this is a cat" );

        String uuid1 = impl.createNewRule( "testLoadArchivedAssets1",
                                           "description",
                                           "testLoadArchivedAssetsCat",
                                           "testLoadArchivedAssetsPackage",
                                           AssetFormats.DRL );
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        repositoryAssetService.archiveAsset( uuid1 );

        String uuid2 = impl.createNewRule( "testLoadArchivedAssets2",
                                           "description",
                                           "testLoadArchivedAssetsCat",
                                           "testLoadArchivedAssetsPackage",
                                           AssetFormats.DRL );
        repositoryAssetService.archiveAsset( uuid2 );

        String uuid3 = impl.createNewRule( "testLoadArchivedAssets3",
                                           "description",
                                           "testLoadArchivedAssetsCat",
                                           "testLoadArchivedAssetsPackage",
                                           AssetFormats.DRL );
        repositoryAssetService.archiveAsset( uuid3 );

        String uuid4 = impl.createNewRule( "testLoadArchivedAssets4",
                                           "description",
                                           "testLoadArchivedAssetsCat",
                                           "testLoadArchivedAssetsPackage",
                                           AssetFormats.DRL );
        repositoryAssetService.archiveAsset( uuid4 );

        String uuid5 = impl.createNewRule( "testLoadArchivedAssets5",
                                           "description",
                                           "testLoadArchivedAssetsCat",
                                           "testLoadArchivedAssetsPackage",
                                           AssetFormats.DRL );
        repositoryAssetService.archiveAsset( uuid5 );

        //We do not know how many archived assets we have in the test repo,
        //but definitely more than 5 (as we just created 5)
        TableDataResult res = repositoryAssetService.loadArchivedAssets( 0,
                                                                         2 );

        assertEquals( 2,
                      res.data.length );
        //may return -1 as per JCR2.0 when precise count is not available due to performance reasons. 
        //assertTrue(-1 != res.total);
        assertTrue( res.currentPosition == 2 );
        assertTrue( res.hasNext );

        res = repositoryAssetService.loadArchivedAssets( 2,
                                                         2 );

        assertEquals( 2,
                      res.data.length );
        //assertTrue(-1 != res.total);
        assertEquals( res.currentPosition,
                      4 );
        assertTrue( res.hasNext );
    }

    @Test
    @Deprecated
    public void testListUnregisteredAssetFormats() throws Exception {
        ServiceImplementation impl = getServiceImplementation();
        PackageItem pkg = impl.getRulesRepository().createPackage( "testListUnregisteredAssetFormats",
                                                                   "" );
        AssetItem as = pkg.addAsset( "whee",
                                     "" );
        as.updateFormat( AssetFormats.DRL );
        as.checkin( "" );

        as = pkg.addAsset( "whee2",
                           "" );
        as.updateFormat( "something_silly" );
        as.checkin( "" );
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        TableDataResult res = repositoryAssetService.listAssets( pkg.getUUID(),
                                                                 new String[0],
                                                                 0,
                                                                 40,
                                                                 ExplorerNodeConfig.RULE_LIST_TABLE_ID );
        assertEquals( 1,
                      res.data.length );
    }

    @Test
    @Ignore("This test is broken. Suspect changes for new Workspaces and versionable packages could have broken.")
    public void testLoadAssetHistoryAndRestore() throws Exception {

        ServiceImplementation impl = getServiceImplementation();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        long startTime = System.currentTimeMillis();
        impl.getRulesRepository().createPackage( "testLoadAssetHistory",
                                                 "desc" );
        long nowTime1 = System.currentTimeMillis();
        System.out.println( "CreatePackage: " + (nowTime1 - startTime) );
        repositoryCategoryService.createCategory( "",
                                                  "testLoadAssetHistory",
                                                  "this is a cat" );

        long nowTime2 = System.currentTimeMillis();
        System.out.println( "CreateCategory: " + (nowTime2 - nowTime1) );
        String uuid = impl.createNewRule( "testLoadAssetHistory",
                                          "description",
                                          "testLoadAssetHistory",
                                          "testLoadAssetHistory",
                                          AssetFormats.DRL );
        long nowTime3 = System.currentTimeMillis();
        System.out.println( "CreateNewRule: " + (nowTime3 - nowTime2) );
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        RuleAsset asset = repositoryAssetService.loadRuleAsset( uuid );
        repositoryAssetService.checkinVersion( asset ); // 1
        long nowTime4 = System.currentTimeMillis();
        System.out.println( "Checkin 1: " + (nowTime4 - nowTime3) );
        asset = repositoryAssetService.loadRuleAsset( uuid );
        long nowTime5 = System.currentTimeMillis();
        System.out.println( "load ruleasset: " + (nowTime5 - nowTime4) );
        repositoryAssetService.checkinVersion( asset ); // 2
        long nowTime6 = System.currentTimeMillis();
        System.out.println( "Checkin 2: " + (nowTime6 - nowTime5) );
        asset = repositoryAssetService.loadRuleAsset( uuid );
        repositoryAssetService.checkinVersion( asset ); // HEAD

        TableDataResult result = repositoryAssetService.loadItemHistory( uuid );
        assertNotNull( result );
        TableDataRow[] rows = result.data;
        assertEquals( 2,
                      rows.length );
        assertFalse( rows[0].id.equals( uuid ) );
        assertFalse( rows[1].id.equals( uuid ) );

        RuleAsset old = repositoryAssetService.loadRuleAsset( rows[0].id );
        RuleAsset newer = repositoryAssetService.loadRuleAsset( rows[1].id );
        assertFalse( old.versionNumber == newer.versionNumber );

        RuleAsset head = repositoryAssetService.loadRuleAsset( uuid );

        long oldVersion = old.versionNumber;
        assertFalse( oldVersion == head.versionNumber );

        repositoryAssetService.restoreVersion( old.uuid,
                                               head.uuid,
                                               "this was cause of a mistake" );

        RuleAsset newHead = repositoryAssetService.loadRuleAsset( uuid );

        assertEquals( "this was cause of a mistake",
                      newHead.checkinComment );

    }

    @Test
    public void testCopyAsset() throws Exception {
        RepositoryService impl = getServiceImplementation();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        repositoryCategoryService.createCategory( "/",
                                                  "templates",
                                                  "ya" );
        String uuid = impl.createNewRule( "testCopyAsset",
                                          "",
                                          "templates",
                                          RulesRepository.DEFAULT_PACKAGE,
                                          AssetFormats.DRL );
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        String uuid2 = repositoryAssetService.copyAsset( uuid,
                                                         RulesRepository.DEFAULT_PACKAGE,
                                                         "testCopyAsset2" );
        assertNotSame( uuid,
                       uuid2 );

        RuleAsset asset = repositoryAssetService.loadRuleAsset( uuid2 );
        assertNotNull( asset );
        assertEquals( RulesRepository.DEFAULT_PACKAGE,
                      asset.metaData.packageName );
        assertEquals( "testCopyAsset2",
                      asset.name );
    }

    @Test
    public void testRemoveAsset() throws Exception {
        RepositoryService impl = getServiceImplementation();
        RepositoryPackageService repositoryPackageService = getRepositoryPackageService();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        String cat = "testRemoveAsset";
        repositoryCategoryService.createCategory( "/",
                                                  cat,
                                                  "ya" );
        String pkgUUID = repositoryPackageService.createPackage( "testRemoveAsset",
                                                                 "" );
        @SuppressWarnings("unused")
        String uuid = impl.createNewRule( "testRemoveAsset",
                                          "x",
                                          cat,
                                          "testRemoveAsset",
                                          "testRemoveAsset" );
        @SuppressWarnings("unused")
        String uuid2 = impl.createNewRule( "testRemoveAsset2",
                                           "x",
                                           cat,
                                           "testRemoveAsset",
                                           "testRemoveAsset" );
        @SuppressWarnings("unused")
        String uuid3 = impl.createNewRule( "testRemoveAsset3",
                                           "x",
                                           cat,
                                           "testRemoveAsset",
                                           "testRemoveAsset" );
        String uuid4 = impl.createNewRule( "testRemoveAsset4",
                                           "x",
                                           cat,
                                           "testRemoveAsset",
                                           "testRemoveAsset" );
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        TableDataResult res = repositoryAssetService.listAssets( pkgUUID,
                                                                 arr( "testRemoveAsset" ),
                                                                 0,
                                                                 -1,
                                                                 ExplorerNodeConfig.RULE_LIST_TABLE_ID );
        assertEquals( 4,
                      res.data.length );

        repositoryAssetService.removeAsset( uuid4 );

        res = repositoryAssetService.listAssets( pkgUUID,
                                                 arr( "testRemoveAsset" ),
                                                 0,
                                                 -1,
                                                 ExplorerNodeConfig.RULE_LIST_TABLE_ID );
        assertEquals( 3,
                      res.data.length );
    }

    @Test
    public void testArchiveAsset() throws Exception {
        RepositoryService impl = getServiceImplementation();
        RepositoryPackageService repositoryPackageService = getRepositoryPackageService();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        String cat = "testArchiveAsset";
        repositoryCategoryService.createCategory( "/",
                                                  cat,
                                                  "ya" );
        String pkgUUID = repositoryPackageService.createPackage( "testArchiveAsset",
                                                                 "" );
        @SuppressWarnings("unused")
        String uuid = impl.createNewRule( "testArchiveAsset",
                                          "x",
                                          cat,
                                          "testArchiveAsset",
                                          "testArchiveAsset" );
        @SuppressWarnings("unused")
        String uuid2 = impl.createNewRule( "testArchiveAsset2",
                                           "x",
                                           cat,
                                           "testArchiveAsset",
                                           "testArchiveAsset" );
        @SuppressWarnings("unused")
        String uuid3 = impl.createNewRule( "testArchiveAsset3",
                                           "x",
                                           cat,
                                           "testArchiveAsset",
                                           "testArchiveAsset" );
        String uuid4 = impl.createNewRule( "testArchiveAsset4",
                                           "x",
                                           cat,
                                           "testArchiveAsset",
                                           "testArchiveAsset" );
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        TableDataResult res = repositoryAssetService.listAssets( pkgUUID,
                                                                 arr( "testArchiveAsset" ),
                                                                 0,
                                                                 -1,
                                                                 ExplorerNodeConfig.RULE_LIST_TABLE_ID );
        assertEquals( 4,
                      res.data.length );
        assertEquals( 4,
                      res.total );
        assertFalse( res.hasNext );

        TableDataResult td = repositoryAssetService.loadArchivedAssets( 0,
                                                                        1000 );
        assertEquals( -1,
                      td.total );
        repositoryAssetService.archiveAsset( uuid4 );

        TableDataResult td2 = repositoryAssetService.loadArchivedAssets( 0,
                                                                         1000 );
        assertTrue( td2.data.length == td.data.length + 1 );

        res = repositoryAssetService.listAssets( pkgUUID,
                                                 arr( "testArchiveAsset" ),
                                                 0,
                                                 -1,
                                                 ExplorerNodeConfig.RULE_LIST_TABLE_ID );
        assertEquals( 3,
                      res.data.length );

        repositoryAssetService.unArchiveAsset( uuid4 );

        res = repositoryAssetService.listAssets( pkgUUID,
                                                 arr( "testArchiveAsset" ),
                                                 0,
                                                 -1,
                                                 ExplorerNodeConfig.RULE_LIST_TABLE_ID );
        assertEquals( 4,
                      res.data.length );

    }

    @Test
    public void testArchiveAssetWhenParentPackageArchived() throws Exception {
        ServiceImplementation impl = getServiceImplementation();
        RepositoryPackageService repositoryPackageService = getRepositoryPackageService();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        String packageName = "testArchiveAssetWhenParentPackageArchived";
        String cat = packageName;
        repositoryCategoryService.createCategory( "/",
                                                  cat,
                                                  "ya" );
        String pkgUUID = repositoryPackageService.createPackage( packageName,
                                                                 "" );
        @SuppressWarnings("unused")
        String uuid = impl.createNewRule( packageName,
                                          "x",
                                          cat,
                                          packageName,
                                          packageName );
        @SuppressWarnings("unused")
        String uuid2 = impl.createNewRule( "testArchiveAssetWhenParentPackageArchived2",
                                           "x",
                                           cat,
                                           packageName,
                                           packageName );
        @SuppressWarnings("unused")
        String uuid3 = impl.createNewRule( "testArchiveAssetWhenParentPackageArchived3",
                                           "x",
                                           cat,
                                           packageName,
                                           packageName );
        String uuid4 = impl.createNewRule( "testArchiveAssetWhenParentPackageArchived4",
                                           "x",
                                           cat,
                                           packageName,
                                           packageName );
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        TableDataResult res = repositoryAssetService.listAssets( pkgUUID,
                                                                 arr( packageName ),
                                                                 0,
                                                                 -1,
                                                                 ExplorerNodeConfig.RULE_LIST_TABLE_ID );
        assertEquals( 4,
                      res.data.length );
        assertEquals( 4,
                      res.total );
        assertFalse( res.hasNext );

        TableDataResult td = repositoryAssetService.loadArchivedAssets( 0,
                                                                        1000 );
        assertEquals( -1,
                      td.total );
        repositoryAssetService.archiveAsset( uuid4 );
        PackageItem packageItem = impl.getRulesRepository().loadPackage( packageName );
        packageItem.archiveItem( true );

        TableDataResult td2 = repositoryAssetService.loadArchivedAssets( 0,
                                                                         1000 );
        assertTrue( td2.data.length == td.data.length + 1 );

        res = repositoryAssetService.listAssets( pkgUUID,
                                                 arr( packageName ),
                                                 0,
                                                 -1,
                                                 ExplorerNodeConfig.RULE_LIST_TABLE_ID );
        assertEquals( 3,
                      res.data.length );

        try {
            repositoryAssetService.unArchiveAsset( uuid4 );
            fail( "Should throw an exception" );
        } catch ( RulesRepositoryException e ) {
            // Works
        }

        res = repositoryAssetService.listAssets( pkgUUID,
                                                 arr( packageName ),
                                                 0,
                                                 -1,
                                                 ExplorerNodeConfig.RULE_LIST_TABLE_ID );
        assertEquals( 3,
                      res.data.length );

    }

    public String[] arr(String s) {
        return new String[]{s};
    }

    @Test
    public void testBuildAssetWithError() throws Exception {
        ServiceImplementation impl = getServiceImplementation();
        RulesRepository repo = impl.getRulesRepository();

        // create our package
        PackageItem pkg = repo.createPackage( "testBuildAssetWithError",
                                              "" );
        AssetItem model = pkg.addAsset( "MyModel",
                                        "" );
        model.updateFormat( AssetFormats.MODEL );
        model.updateBinaryContentAttachment( this.getClass().getResourceAsStream( "/billasurf.jar" ) );
        model.checkin( "" );

        DroolsHeader.updateDroolsHeader( "import com.billasurf.Person",
                                                  pkg );

        AssetItem asset = pkg.addAsset( "testRule",
                                        "" );
        asset.updateFormat( AssetFormats.DRL );
        asset.updateContent( "rule 'MyGoodRule' \n when Personx() then System.err.println(42); \n end" );
        asset.checkin( "" );
        repo.save();
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        RuleAsset rule = repositoryAssetService.loadRuleAsset( asset.getUUID() );

        BuilderResult result = repositoryAssetService.validateAsset( rule );
        assertNotNull( result );
        assertEquals( -1,
                      result.getLines().get( 0 ).getMessage().indexOf( "Check log for" ) );
        assertTrue( result.getLines().get( 0 ).getMessage().indexOf( "Unable to resolve" ) > -1 );

    }

    @Test
    public void testBuildAsset() throws Exception {
        ServiceImplementation impl = getServiceImplementation();
        RulesRepository repo = impl.getRulesRepository();

        // create our package
        PackageItem pkg = repo.createPackage( "testBuildAsset",
                                              "" );
        AssetItem model = pkg.addAsset( "MyModel",
                                        "" );
        model.updateFormat( AssetFormats.MODEL );
        model.updateBinaryContentAttachment( this.getClass().getResourceAsStream( "/billasurf.jar" ) );
        model.checkin( "" );

        DroolsHeader.updateDroolsHeader( "import com.billasurf.Person",
                                                  pkg );

        AssetItem asset = pkg.addAsset( "testRule",
                                        "" );
        asset.updateFormat( AssetFormats.DRL );
        asset.updateContent( "rule 'MyGoodRule' \n when Person() then System.err.println(42); \n end" );
        asset.checkin( "" );
        repo.save();
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        RuleAsset rule = repositoryAssetService.loadRuleAsset( asset.getUUID() );

        // check its all OK
        BuilderResult result = repositoryAssetService.validateAsset( rule );
        assertTrue(result.getLines().isEmpty());

        RuleBaseCache.getInstance().clearCache();

        // try it with a bad rule
        RuleContentText text = new RuleContentText();
        text.content = "rule 'MyBadRule' \n when Personx() then System.err.println(42); \n end";
        rule.content= text;

        result = repositoryAssetService.validateAsset( rule );
        assertNotNull( result );
        assertNotNull( result.getLines().get( 0 ).getMessage() );
        assertEquals( AssetFormats.DRL,
                      result.getLines().get( 0 ).getAssetFormat() );

        // now mix in a DSL
        AssetItem dsl = pkg.addAsset( "MyDSL",
                                      "" );
        dsl.updateFormat( AssetFormats.DSL );
        dsl.updateContent( "[when]There is a person=Person()\n[then]print out 42=System.err.println(42);" );
        dsl.checkin( "" );

        AssetItem dslRule = pkg.addAsset( "dslRule",
                                          "" );
        dslRule.updateFormat( AssetFormats.DSL_TEMPLATE_RULE );
        dslRule.updateContent( "when \n There is a person \n then \n print out 42" );
        dslRule.checkin( "" );

        rule = repositoryAssetService.loadRuleAsset( dslRule.getUUID() );

        result = repositoryAssetService.validateAsset( rule );
        assertTrue(result.getLines().isEmpty());

        asset = pkg.addAsset( "someEnumThing",
                              "" );
        asset.updateFormat( AssetFormats.ENUMERATION );
        asset.updateContent( "goober boy" );
        asset.checkin( "" );
        result = repositoryAssetService.validateAsset( repositoryAssetService.loadRuleAsset( asset.getUUID() ) );
        assertFalse( result.getLines().size() == 0 );

    }

    @Test
    public void testBuildAssetMultipleFunctionsCallingEachOther() throws Exception {

        ServiceImplementation impl = getServiceImplementation();
        RepositoryPackageService repositoryPackageService = getRepositoryPackageService();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        repositoryPackageService.createPackage( "testBuildAssetMultipleFunctionsCallingEachOther",
                                                "" );
        repositoryCategoryService.createCategory( "/",
                                                  "funkytest",
                                                  "" );

        String uuidt1 = impl.createNewRule( "t1",
                                            "",
                                            "funkytest",
                                            "testBuildAssetMultipleFunctionsCallingEachOther",
                                            AssetFormats.FUNCTION );
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        RuleAsset t1 = repositoryAssetService.loadRuleAsset( uuidt1 );
        RuleContentText t1Content = new RuleContentText();
        t1Content.content = "function void t1(){\n";
        t1Content.content += " t2();\n";
        t1Content.content += "}\n";
        t1.content= t1Content;
        repositoryAssetService.checkinVersion( t1 );

        String uuidt2 = impl.createNewRule( "t2",
                                            "",
                                            "funkytest",
                                            "testBuildAssetMultipleFunctionsCallingEachOther",
                                            AssetFormats.FUNCTION );
        RuleAsset t2 = repositoryAssetService.loadRuleAsset( uuidt2 );
        RuleContentText t2Content = new RuleContentText();
        t2Content.content = "function void t2(){\n";
        t2Content.content += " t1();\n";
        t2Content.content += "}\n";
        t2.content = t2Content;
        repositoryAssetService.checkinVersion( t2 );

        BuilderResult result = repositoryAssetService.validateAsset( t1 );

        assertTrue(result.getLines().isEmpty());

    }

    @Test
    public void testBuildAssetBRXMLAndCopy() throws Exception {
        ServiceImplementation impl = getServiceImplementation();
        RepositoryPackageService repositoryPackageService = getRepositoryPackageService();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        RulesRepository repo = impl.getRulesRepository();

        // create our package
        PackageItem pkg = repo.createPackage( "testBuildAssetBRL",
                                              "" );
        AssetItem model = pkg.addAsset( "MyModel",
                                        "" );
        model.updateFormat( AssetFormats.MODEL );
        model.updateBinaryContentAttachment( this.getClass().getResourceAsStream( "/billasurf.jar" ) );
        model.checkin( "" );

        DroolsHeader.updateDroolsHeader( "import com.billasurf.Person",
                                                  pkg );
        repositoryCategoryService.createCategory( "/",
                                                  "brl",
                                                  "" );

        String uuid = impl.createNewRule( "testBRL",
                                          "",
                                          "brl",
                                          "testBuildAssetBRL",
                                          AssetFormats.BUSINESS_RULE );
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        RuleAsset rule = repositoryAssetService.loadRuleAsset( uuid );

        RuleModel m = (RuleModel) rule.content;
        assertNotNull( m );
        m.name = "testBRL";

        FactPattern p = new FactPattern( "Person" );
        p.setBoundName( "p" );
        SingleFieldConstraint con = new SingleFieldConstraint();
        con.setFieldName( "name" );
        con.setValue( "mark" );
        con.setOperator( "==" );
        con.setConstraintValueType( SingleFieldConstraint.TYPE_LITERAL );
        con.setFieldType( SuggestionCompletionEngine.TYPE_STRING );

        p.addConstraint( con );

        m.addLhsItem( p );

        ActionSetField set = new ActionSetField( "p" );
        ActionFieldValue f = new ActionFieldValue( "name",
                                                   "42-ngoo",
                                                   SuggestionCompletionEngine.TYPE_STRING );
        set.addFieldValue( f );

        m.addRhsItem( set );

        repositoryAssetService.checkinVersion( rule );

        // check its all OK
        BuilderResult result = repositoryAssetService.validateAsset( rule );
        assertTrue(result.getLines().isEmpty());

        List<AssetItem> assets = iteratorToList( pkg.getAssets() );
        assertEquals( 3,
                      assets.size() );
        // now lets copy...
        String newUUID = repositoryAssetService.copyAsset( rule.uuid,
                                                           rule.metaData.packageName,
                                                           "ruleName2" );

        assets = iteratorToList( pkg.getAssets() );
        assertEquals( 4,
                      assets.size() ); //we have 4 due to the drools.package file.
        RuleAsset asset = repositoryAssetService.loadRuleAsset( newUUID );

        String pkgSource = repositoryPackageService.buildPackageSource( pkg.getUUID() );

        assertTrue( pkgSource.indexOf( "ruleName2" ) > 0 );
        assertTrue( repositoryAssetService.buildAssetSource( asset ).indexOf( "ruleName2" ) > 0 );
        assertTrue( repositoryAssetService.buildAssetSource( asset ).indexOf( "testBRL" ) == -1 );

        // RuleModel model2 = (RuleModel) asset.content;
        // assertEquals("ruleName2", model2.name);

    }

    private List<AssetItem> iteratorToList(Iterator<AssetItem> assets) {
        List<AssetItem> result = new ArrayList<AssetItem>();
        while ( assets.hasNext() ) {
            result.add( assets.next() );

        }
        return result;
    }

    @Test
    public void testAssetSource() throws Exception {
        ServiceImplementation impl = getServiceImplementation();
        RulesRepository repo = impl.getRulesRepository();

        // create our package
        PackageItem pkg = repo.createPackage( "testAssetSource",
                                              "" );
        AssetItem asset = pkg.addAsset( "testRule",
                                        "" );
        asset.updateFormat( AssetFormats.DRL );
        asset.updateContent( "rule 'n' \n when Foo() then bar(); \n end" );
        asset.checkin( "" );
        repo.save();
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        RuleAsset rule = repositoryAssetService.loadRuleAsset( asset.getUUID() );
        String drl = repositoryAssetService.buildAssetSource( rule );
        assertEquals( "rule 'n' \n when Foo() then bar(); \n end",
                      drl );

        asset = pkg.addAsset( "DT",
                              "" );
        asset.updateFormat( AssetFormats.DECISION_SPREADSHEET_XLS );
        asset.updateBinaryContentAttachment( this.getClass().getResourceAsStream( "/SampleDecisionTable.xls" ) );
        asset.checkin( "" );

        rule = repositoryAssetService.loadRuleAsset( asset.getUUID() );
        drl = repositoryAssetService.buildAssetSource( rule );
        assertNotNull( drl );
        assertTrue( drl.indexOf( "rule" ) > -1 );
        assertTrue( drl.indexOf( "policy: Policy" ) > -1 );

        AssetItem dsl = pkg.addAsset( "MyDSL",
                                      "" );
        dsl.updateFormat( AssetFormats.DSL );
        dsl.updateContent( "[when]This is foo=bar()\n[then]do something=yeahMan();" );
        dsl.checkin( "" );

        asset = pkg.addAsset( "MyDSLRule",
                              "" );
        asset.updateFormat( AssetFormats.DSL_TEMPLATE_RULE );
        asset.updateContent( "when \n This is foo \n then \n do something" );
        asset.checkin( "" );

        rule = repositoryAssetService.loadRuleAsset( asset.getUUID() );
        drl = repositoryAssetService.buildAssetSource( rule );
        assertNotNull( drl );
        assertTrue( drl.indexOf( "This is foo" ) == -1 );
        assertTrue( drl.indexOf( "do something" ) == -1 );
        assertTrue( drl.indexOf( "bar()" ) > -1 );
        assertTrue( drl.indexOf( "yeahMan();" ) > -1 );

        rule = repositoryAssetService.loadRuleAsset( repo.copyAsset( asset.getUUID(),
                                                                     "testAssetSource",
                                                                     "newRuleName" ) );
        // System.err.println(((RuleContentText)rule.content).content);
        drl = repositoryAssetService.buildAssetSource( rule );
        assertNotNull( drl );
        assertTrue( drl.indexOf( "newRuleName" ) > 0 );

    }

    @Test
    public void testBuildAssetWithPackageConfigError() throws Exception {
        ServiceImplementation impl = getServiceImplementation();
        RulesRepository repo = impl.getRulesRepository();

        PackageItem pkg = repo.createPackage( "testBuildAssetWithPackageConfigError",
                                              "" );
        // AssetItem model = pkg.addAsset( "MyModel", "" );
        // model.updateFormat( AssetFormats.MODEL );
        // model.updateBinaryContentAttachment(
        // this.getClass().getResourceAsStream( "/billasurf.jar" ) );
        // model.checkin( "" );

        // pkg.updateHeader( "import com.billasurf.Person" );

        AssetItem asset = pkg.addAsset( "testRule",
                                        "" );
        asset.updateFormat( AssetFormats.DRL );
        asset.updateContent( "rule 'MyGoodRule' \n when \n then \n end" );
        asset.checkin( "" );
        repo.save();
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        RuleAsset rule = repositoryAssetService.loadRuleAsset( asset.getUUID() );

        // check its all OK
        BuilderResult result = repositoryAssetService.validateAsset( rule );
        assertTrue(result.getLines().isEmpty());

        DroolsHeader.updateDroolsHeader( "importxxxx",
                                                  pkg );
        repo.save();
        result = repositoryAssetService.validateAsset( rule );
        assertNotNull( result );

        assertEquals( 2,
                      result.getLines().size() );
        assertEquals( "package",
                      result.getLines().get( 0 ).getAssetFormat() );
        assertNotNull( result.getLines().get( 0 ).getMessage() );
        assertEquals( "package",
                      result.getLines().get( 1 ).getAssetFormat() );
        assertNotNull( result.getLines().get( 1 ).getMessage() );

    }

    @Test
    public void testLoadArchivedAssetsPagedResults() throws Exception {

        final int PAGE_SIZE = 2;

        ServiceImplementation impl = getServiceImplementation();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        impl.getRulesRepository().createPackage( "testLoadArchivedAssetsPackage",
                                                 "desc" );
        repositoryCategoryService.createCategory( "",
                                                  "testLoadArchivedAssetsCat",
                                                  "this is a cat" );

        String uuid1 = impl.createNewRule( "testLoadArchivedAssets1",
                                           "description",
                                           "testLoadArchivedAssetsCat",
                                           "testLoadArchivedAssetsPackage",
                                           AssetFormats.DRL );
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        repositoryAssetService.archiveAsset( uuid1 );

        String uuid2 = impl.createNewRule( "testLoadArchivedAssets2",
                                           "description",
                                           "testLoadArchivedAssetsCat",
                                           "testLoadArchivedAssetsPackage",
                                           AssetFormats.DRL );
        repositoryAssetService.archiveAsset( uuid2 );

        String uuid3 = impl.createNewRule( "testLoadArchivedAssets3",
                                           "description",
                                           "testLoadArchivedAssetsCat",
                                           "testLoadArchivedAssetsPackage",
                                           AssetFormats.DRL );
        repositoryAssetService.archiveAsset( uuid3 );

        PageRequest request = new PageRequest( 0,
                                               PAGE_SIZE );
        PageResponse<AdminArchivedPageRow> response;
        response = repositoryAssetService.loadArchivedAssets( request );

        assertNotNull( response );
        assertNotNull( response.getPageRowList() );
        assertTrue( response.getStartRowIndex() == 0 );
        assertTrue( response.getPageRowList().size() == PAGE_SIZE );
        assertFalse( response.isLastPage() );

        request.setStartRowIndex( PAGE_SIZE );
        response = repositoryAssetService.loadArchivedAssets( request );

        assertNotNull( response );
        assertNotNull( response.getPageRowList() );
        assertTrue( response.getStartRowIndex() == PAGE_SIZE );
        assertTrue( response.getPageRowList().size() == 1 );
        assertTrue( response.isLastPage() );
    }

    @Test
    public void testLoadArchivedAssetsFullResults() throws Exception {

        ServiceImplementation impl = getServiceImplementation();
        RepositoryCategoryService repositoryCategoryService = getRepositoryCategoryService();
        impl.getRulesRepository().createPackage( "testLoadArchivedAssetsPackage",
                                                 "desc" );
        repositoryCategoryService.createCategory( "",
                                                  "testLoadArchivedAssetsCat",
                                                  "this is a cat" );

        String uuid1 = impl.createNewRule( "testLoadArchivedAssets1",
                                           "description",
                                           "testLoadArchivedAssetsCat",
                                           "testLoadArchivedAssetsPackage",
                                           AssetFormats.DRL );
        RepositoryAssetService repositoryAssetService = getRepositoryAssetService();
        repositoryAssetService.archiveAsset( uuid1 );

        String uuid2 = impl.createNewRule( "testLoadArchivedAssets2",
                                           "description",
                                           "testLoadArchivedAssetsCat",
                                           "testLoadArchivedAssetsPackage",
                                           AssetFormats.DRL );
        repositoryAssetService.archiveAsset( uuid2 );

        String uuid3 = impl.createNewRule( "testLoadArchivedAssets3",
                                           "description",
                                           "testLoadArchivedAssetsCat",
                                           "testLoadArchivedAssetsPackage",
                                           AssetFormats.DRL );
        repositoryAssetService.archiveAsset( uuid3 );

        PageRequest request = new PageRequest( 0,
                                               null );
        PageResponse<AdminArchivedPageRow> response;
        response = repositoryAssetService.loadArchivedAssets( request );

        assertNotNull( response );
        assertNotNull( response.getPageRowList() );
        assertTrue( response.getStartRowIndex() == 0 );
        assertTrue( response.getPageRowList().size() == 3 );
        assertTrue( response.isLastPage() );
    }

}
