package com.asiainfo.billing.drquery.connection;

import java.sql.Connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.asiainfo.billing.drquery.utils.ServiceLocator;

public class DRConnection {
	
	private final static Log log = LogFactory.getLog(DRConnection.class);

	private ConnectionFactory factory;
	
	private ConnectionHolder holder;
	
	private Connection navtiveConnection;
	
	private String factoryName;
	
	
	/**
	 * 根据连接类型获得Connection
	 * @param connectionType
	 * @return
	 * @throws ConnectionException
	 */
	public Connection getConnectionFromFactory(String factoryName) throws ConnectionException{
//		if(factoryName != null && !factoryName.equals(this.factoryName) && this.factoryName != null){
//			releaseConnection();
//		}
//		if(factoryName != null && factoryName.equals(this.factoryName) && navtiveConnection != null){
//			return navtiveConnection;
//		}
		this.factoryName = factoryName;
		factory = ServiceLocator.getInstance().getService(factoryName, ConcreteConnctionFactory.class);
		try {
			holder = factory.getConnectionHolder();
			//factory.getPool().getResource();
			navtiveConnection = (Connection) holder.getNatvieConnection();
		} catch (ConnectionException e) {
			throw new ConnectionException("get db connection failed", e);
		}
		return navtiveConnection;
	}
	
	
	/**
	 * 释放连接
	 */
	public void releaseConnection(){
		try {
			ConnectionUtils.releaseConnection(holder, factory);
		} catch (ConnectionException e) {
			log.error(e);
		}
	}


	public ConnectionFactory getFactory() {
		return factory;
	}


	public void setFactory(ConnectionFactory factory) {
		this.factory = factory;
	}


	public ConnectionHolder getHolder() {
		return holder;
	}


	public void setHolder(ConnectionHolder holder) {
		this.holder = holder;
	}
	
	
	
	
}
