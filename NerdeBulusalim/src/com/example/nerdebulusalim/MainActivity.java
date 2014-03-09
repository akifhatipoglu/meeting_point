package com.example.nerdebulusalim;
//add
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import com.example.nerdebulusalim.overlay.addressOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends MapActivity {
	private static final int PICK_CONTACT = 0;
	private MapView mapView;
	private EditText search;
	private Button searchButton;
	private Address address;
	private ArrayList<String> numbers;
	private ArrayList<String> names;
	private String celcius = "";
	private Context context;
	/*final static String NAMESPACE = "http://tempuri.org/";
	final static String METHOD_NAME = "CelsiusToFahrenheit";
	final static String SOAP_ACTION = "http://tempuri.org/CelsiusToFahrenheit";
	final static String URL = "http://www.w3schools.com/webservices/tempconvert.asmx?WSDL";*/
	final static String NAMESPACE = "http://v3.soap.location.capabilityexposure.services.oksijen.com";
	final static String METHOD_NAME = "getLocation";
	final static String SOAP_ACTION = "http://v3.soap.location.capabilityexposure.services.oksijen.com/getLocation";
	final static String URL = "http://ulusoyweb.net/wsdl_deneme/TerminalLocationV3?WSDL";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setupViews();
		numbers = new ArrayList<String>();
		names = new ArrayList<String>();
		context = this;
	}
	

	private void setupViews() {
		/*
		 * Vodafone apisi ile konum bilgisi gelecek*/
		search = (EditText)findViewById(R.id.search);
		searchButton = (Button)findViewById(R.id.searchButton);
		searchButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mapAddress();
			}
		});
		mapView = (MapView)findViewById(R.id.map);
		mapView.setBuiltInZoomControls(true);
	}
	protected void mapAddress() {
		/*
		 * Versiyon ikide 1 den fazla sonuç alýnacak ve kullanýcý onlardan birini seçecek*/
		String addressString = search.getText().toString();
		Geocoder g = new Geocoder(this);
		List<Address> addresses;
		try{
			addresses = g.getFromLocationName(addressString, 1);//second parameter is number of results 
			if(addresses!=null && addresses.size()>0){
				List<Overlay> overlayList = mapView.getOverlays();
				address = addresses.get(0);//address contains latitude and longitude inofmations
				addressOverlay overlay = new addressOverlay(address,getBaseContext());
				overlayList.clear();
				overlayList.add(overlay);
				mapView.invalidate();//invokes draw method if view is visible
				final MapController controller = mapView.getController();
				controller.animateTo(overlay.getGeoPoint(), new Runnable() {
					
					public void run() {
						controller.setZoom(15);
					}
				});
			}else{
				//There is no result for this address
			}
		}catch(IOException ex){	//if network connection nof found or permissions are not allowed
			ex.printStackTrace();
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	protected void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);
		
		String name;
		String phoneNumber;
		  switch (reqCode) {
		    case (PICK_CONTACT) :
		      if (resultCode == MapActivity.RESULT_OK) {
		        Uri contactData = data.getData();
		        Cursor cursor =  getContentResolver().query(contactData, null, null, null, null);
		        if(cursor.moveToNext()){
		        	String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID)); 
		        	String hasPhone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)); 
		        	if (hasPhone.equals("1")) { 
		        		// You know it has a number so now query it like this
		        		Cursor phones = getContentResolver().query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId, null, null); 
		        		while (phones.moveToNext()) { 
		        			phoneNumber = phones.getString(phones.getColumnIndex( ContactsContract.CommonDataKinds.Phone.NUMBER));   
		        			name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
		        			if(!numbers.contains(phoneNumber))
		        				numbers.add(phoneNumber);
		        			if(!names.contains(name))
		        				names.add(name);
		        			openDialog();
		        			break;
		        		} 
		        		phones.close(); 
		        	}
		        }
		      }
		      break;
		  }
	}


	private void openDialog() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(names.toString());
		buffer.deleteCharAt(0);
		buffer.deleteCharAt(buffer.length()-1);
		AlertDialog dialog = new AlertDialog.Builder(this)
		.setTitle("Mesaj gonderilsin mi?")
		.setMessage(buffer.toString())
		.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PullCoordinatesAsync task = new PullCoordinatesAsync();
				task.execute();
				dialog.cancel();
			}

		}).setNegativeButton("Iptal", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		}).setNeutralButton("Kisi Ekle", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(Intent.ACTION_PICK,ContactsContract.Contacts.CONTENT_URI);
	        	startActivityForResult(intent, PICK_CONTACT);
	        	dialog.cancel();
			}
		}).create();
		dialog.show();
	}

	private String getLocation() {
		String derece = "";
		float Latitude = 14.0F,Longitude = 14.0F;
		SoapObject request = new SoapObject(NAMESPACE,METHOD_NAME);
		//SoapObject requestTag = new SoapObject(NAMESPACE,"address");
		//requestTag.addProperty("usageId","usage id deðeri buraya gelecek");
		//requestTag.addProperty("endUserId", "key value buraya gelecek");
		//request.addSoapObject(requestTag);
		//request.addProperty("Celsius","100");
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
    	envelope.setOutputSoapObject(request);
    	envelope.dotNet = false;
    	HttpTransportSE ht = new HttpTransportSE(URL);//60000 is time out
		try {
			ht.call(SOAP_ACTION, envelope);
			SoapObject result = (SoapObject)envelope.bodyIn;
			SoapObject responseProperty = (SoapObject)result.getProperty("return");
			SoapObject response = (SoapObject)responseProperty.getProperty("currentLocation");
			//derece = result.getProperty("CelsiusToFahrenheitResult").toString();
			//return derece;
			Latitude = Float.parseFloat(response.getProperty("latitude").toString());
			Longitude = Float.parseFloat(response.getProperty("longitude").toString());
			//SoapObject object = (SoapObject)response.getProperty("getLocationResponse");
			//Latitude = Float.parseFloat(object.getProperty("latitude").toString());
			//Longitude = Float.parseFloat(object.getProperty("longitude").toString());
			//Toast.makeText(this, String.valueOf(Latitude), (int)3).show();
			derece = "Latitude: " + String.valueOf(Latitude) + " Longitude: " + String.valueOf(Longitude);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		return derece;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
        {
        case R.id.satallite:
        	 if (item.isChecked()) item.setChecked(false);
             else item.setChecked(true);
        	 mapView.setSatellite(true);
            return true;
        case R.id.map:
        	 if (item.isChecked()) item.setChecked(false);
             else item.setChecked(true);
        	 mapView.setSatellite(false);
        	 mapView.setTraffic(false);
        	return true;
        case R.id.traffic:
        	 if (item.isChecked()) item.setChecked(false);
             else item.setChecked(true);
        	 mapView.setTraffic(true);
        	 return true;
        case R.id.exit:
        	this.finish();
        	return true;
        case R.id.send:
        	Intent intent = new Intent(Intent.ACTION_PICK,ContactsContract.Contacts.CONTENT_URI);
        	startActivityForResult(intent, PICK_CONTACT);
        	return true;
        default:
		return super.onOptionsItemSelected(item);
        }	
	}
	private class PullCoordinatesAsync extends AsyncTask<Void, Void, String>{

		@Override
		protected String doInBackground(Void... params) {
			//celcius = getLocation();
			return celcius;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			Toast.makeText(context, celcius, (int)5).show();
		}
		
		
	}
}
