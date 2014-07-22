/*
 * Copyright (c) 2012, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.samples.templateapp;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.sfnative.SalesforceActivity;
import com.salesforce.androidsdk.util.EventsObservable;
import com.salesforce.androidsdk.util.EventsObservable.EventType;

/**
 * Main activity
 */
public class MainActivity extends SalesforceActivity {

    private RestClient client;
	private String partidoJugadoID;
	private String partidoJugadoMessage;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Setup view
		setContentView(R.layout.main);
	}
	
	@Override 
	public void onResume() {
		// Hide everything until we are logged in
		findViewById(R.id.root).setVisibility(View.INVISIBLE);

		TextView message = (TextView) findViewById(R.id.PartidoDetails);
		message.setText(partidoJugadoMessage);				
		
		super.onResume();
	}		
	
	@Override
	public void onResume(RestClient client) {
        // Keeping reference to rest client
        this.client = client; 

		// Show everything
		findViewById(R.id.root).setVisibility(View.VISIBLE);
	}

	/**
	 * Called when "Logout" button is clicked. 
	 * 
	 * @param v
	 */
	public void onLogoutClick(View v) {
		SalesforceSDKManager.getInstance().logout(this);
	}	

	/**
	 * Called when "Fetch Contacts" button is clicked
	 * 
	 * @param v
	 * @throws UnsupportedEncodingException 
	 */
	public void onPartidosJugadosClick(View v) throws UnsupportedEncodingException {
        String sqlPartidosJugados = "SELECT Id, JugadorID__r.Name, Estado__c, PartidoID__r.Name, PartidoID__r.Fecha__c FROM PartidoJugado__c WHERE PartidoID__r.Fecha__c >= TODAY and JugadorID__c = '" + Constants.jugadorID + "' order by PartidoID__r.Fecha__c desc limit 1";
        RestRequest restRequest = RestRequest.getRequestForQuery(getString(R.string.api_version), sqlPartidosJugados);
        
        System.out.println(sqlPartidosJugados);
        
		client.sendAsync(restRequest, new AsyncRequestCallback() {
			@Override
			public void onSuccess(RestRequest request, RestResponse result) {
				try {
					JSONArray records = result.asJSONObject().getJSONArray("records");
					if (records.length() > 0) {
						Integer i = 0;
						partidoJugadoID = records.getJSONObject(i).getString("Id");
						String nombreRival = records.getJSONObject(i).getJSONObject("PartidoID__r").getString("Name");
						partidoJugadoMessage = "Tu estado para el Partido Contra \"" + nombreRival + "\" es " + records.getJSONObject(i).getString("Estado__c");	
						System.out.println(partidoJugadoMessage);
						
						TextView message = (TextView) findViewById(R.id.PartidoDetails);
						message.setText(partidoJugadoMessage);		
					}
				} catch (Exception e) {
					onError(e);
				}
			}
			
			@Override
			public void onError(Exception exception) {
                Toast.makeText(MainActivity.this,
                               MainActivity.this.getString(SalesforceSDKManager.getInstance().getSalesforceR().stringGenericError(), exception.toString()),
                               Toast.LENGTH_LONG).show();
			}
		});
	}
	
	public void onUpdateClick(View v, String estado) {
		Map<String, Object> fields = new HashMap<String, Object>();
		fields.put("Estado__c", estado);
		
		RestRequest request = null;
		try {
			request = RestRequest.getRequestForUpdate(getString(R.string.api_version), "PartidoJugado__c", partidoJugadoID, fields);
		} catch (Exception e) {
			System.out.println(e);
			return;
		}
		sendRequest(request);
	}
	
	private void sendRequest(RestRequest restRequest) {
		client.sendAsync(restRequest, new AsyncRequestCallback() {
			
			@Override
			public void onSuccess(RestRequest request, RestResponse result) {
				try {
					System.out.println(result);
				} catch (Exception e) {
					onError(e);
				}
				
				EventsObservable.get().notifyEvent(EventType.RenditionComplete);
			}
			
			@Override
			public void onError(Exception exception) {
				onError(exception);
				EventsObservable.get().notifyEvent(EventType.RenditionComplete);	
			}
		});
	}
	
	public void onParticipacionEnDudaClick (View v) {
		onUpdateClick(v, "En duda");
	}

	public void onParticipacionSiClick (View v) {
		onUpdateClick(v, "Si");
	}
	
	public void onParticipacionNoClick (View v) {
		onUpdateClick(v, "No");
	}
	
	public void onParticipacionIgnorarClick (View v) {
		// do nothing
	}
	
}
