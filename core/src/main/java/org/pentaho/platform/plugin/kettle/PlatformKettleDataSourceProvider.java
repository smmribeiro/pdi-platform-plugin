/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.platform.plugin.kettle;

import org.pentaho.di.core.database.DataSourceNamingException;
import org.pentaho.di.core.database.DataSourceProviderFactory;
import org.pentaho.di.core.database.DataSourceProviderInterface;
import org.pentaho.platform.api.data.DBDatasourceServiceException;
import org.pentaho.platform.api.data.IDBDatasourceService;
import org.pentaho.platform.api.data.IJndiDatasourceService;
import org.pentaho.platform.api.data.IPooledDatasourceService;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import javax.sql.DataSource;

public class PlatformKettleDataSourceProvider implements DataSourceProviderInterface {

  protected static final PlatformKettleDataSourceProvider instance = new PlatformKettleDataSourceProvider();

  private PlatformKettleDataSourceProvider() {
    // Private constructor
  }

  public DataSourceProviderInterface getInstance() {
    return instance;
  }

  protected static void hookupProvider() {
    DataSourceProviderFactory.setDataSourceProviderInterface( instance );
  }

  private <T extends IDBDatasourceService> IDBDatasourceService getService( Class<T> dataSourceServiceInterface ) {

    T datasourceService = PentahoSystem.get( dataSourceServiceInterface, null );

    IDBDatasourceService service = ( datasourceService == null )
      ? PentahoSystem.get( IDBDatasourceService.class, null )
      : datasourceService;

    return service;
  }

  protected <T extends IDBDatasourceService> DataSource getNamedDataSourceFromService(
    Class<T> dataSourceServiceInterface, String dataSourceName ) throws DataSourceNamingException {

    IDBDatasourceService service = getService( dataSourceServiceInterface );
    if ( service != null ) {
      try {
        return service.getDataSource( dataSourceName );
      } catch ( DBDatasourceServiceException ex ) {
        throw new DataSourceNamingException( ex );
      }
    }
    return null;
  }

  public DataSource getNamedDataSource( String dataSourceName ) throws DataSourceNamingException {
    return getNamedDataSourceFromService( IDBDatasourceService.class, dataSourceName );
  }

  @Override
  public DataSource getNamedDataSource( String dataSourceName, DataSourceProviderInterface.DatasourceType type )
    throws DataSourceNamingException {
    if ( type != null ) {
      switch ( type ) {
        case JNDI:
          return getNamedDataSourceFromService( IJndiDatasourceService.class, dataSourceName );
        case POOLED:
          return getNamedDataSourceFromService( IPooledDatasourceService.class, dataSourceName );
      }
    }
    throw new DataSourceNamingException(
      String.format( "Unknown data source type [%s] for named data source [%s]", type, dataSourceName ) );
  }


  @Override public DataSource invalidateNamedDataSource( String datasourceName, DatasourceType type )
    throws DataSourceNamingException {

    IDBDatasourceService service;

    switch ( type ) {
      case JNDI:
        service = getService( IJndiDatasourceService.class );
        break;
      case POOLED:
        service = getService( IPooledDatasourceService.class );
        break;
      default:
        service = getService( IDBDatasourceService.class );
    }

    if ( service != null ) {
      try {
        DataSource dataSource = service.getDataSource( datasourceName );
        if ( dataSource == null ) {
          return null;
        }
        service.clearDataSource( datasourceName );
        return dataSource;
      } catch ( DBDatasourceServiceException ex ) {
        throw new DataSourceNamingException( ex );
      }
    }
    return null;
  }
}
