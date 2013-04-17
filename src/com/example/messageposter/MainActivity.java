package com.example.messageposter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class MainActivity extends Activity {
	
	List<Map<String, String>> data = new ArrayList<Map<String, String>>();
	SimpleAdapter adapter;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //create a simple adapter
        adapter = new SimpleAdapter(this, 
        		data,							 //a list of hashmaps
                R.layout.message_item, 			 //the layout to use for each item
                new String[] {"Name", "Comment", "ID" }, 	 //the array of keys
                new int[] {R.id.nameText, R.id.commentText, R.id.hiddenId });	//array of view ids that should display the values (same order)
        ListView messageList = (ListView)findViewById(R.id.currentmessages);
        messageList.setAdapter(adapter);
        messageList.setLongClickable(true);
        
        //on long click, offer to delete item in a popup.
        messageList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

        	public boolean onItemLongClick(AdapterView<?> listView, View v, int position,
					long id) {
		
        		
				final HashMap<String,String> item = (HashMap)listView.getItemAtPosition(position);		
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				
				// Add the Delete button
				builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
						   
				           public void onClick(DialogInterface dialog, int id) {
				        	   DeleteTask task = new DeleteTask();
								task.execute(item.get("ID"));
				           }
				       });
				
				//Add the Cancel button
				builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	  dialog.cancel();
				           }
				       });
				
				// Set other dialog properties
				builder.setTitle("Delete Item");
				
				// Create the AlertDialog and show it
				AlertDialog dialog = builder.create();
				dialog.show();
				return false;
        	}
        }); 
			
        reload(null);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    //post values from the entry form to the server
    public void post(View v) {
    	EditText nameBox = (EditText)findViewById(R.id.namebox);
    	String name = nameBox.getText().toString();
    	
    	EditText messageBox = (EditText)findViewById(R.id.messagebox);
    	String message = messageBox.getText().toString();
    	
    	PostTask task = new PostTask();
    	task.execute(name, message);
    }
    
    //fetch the list of messages from the server
    public void reload(View v) {
    	LoadTask task = new LoadTask();
    	task.execute();
    }
    
    private class PostTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {

			String url = "http://messagestore.herokuapp.com/messages";

			HttpResponse response;
			HttpClient httpclient = new DefaultHttpClient();
			try {
				 
				 HttpPost post = new HttpPost(url);
			     List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			     postParameters.add(new BasicNameValuePair("name", params[0]));
			     postParameters.add(new BasicNameValuePair("comment", params[1]));

			     UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParameters);
		         post.setEntity(entity);

 	             response = httpclient.execute(post);
 	            
 	        } catch (ClientProtocolException e) {
 	            //TODO Handle problems..
 	        } catch (IOException e) {
 	            //TODO Handle problems..
 	        }
			
			 return null;
		}
		
		@Override
		protected void onPostExecute(String arg0) {
			reload(null);
		}
    	
    }
    
    private class DeleteTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			String id = params[0];
			String url = "http://messagestore.herokuapp.com/messages/" + id;
			HttpResponse response;
			HttpClient httpclient = new DefaultHttpClient();
			try {
				 HttpDelete delete = new HttpDelete(url);
 	             response = httpclient.execute(delete);
 	            
 	        } catch (ClientProtocolException e) {
 	            //TODO Handle problems..
 	        } catch (IOException e) {
 	            //TODO Handle problems..
 	        }			
			 return null;
		}
		
		@Override
		protected void onPostExecute(String arg0) {
			reload(null);
		}
    	
    }
    
    private class LoadTask extends AsyncTask<Void, Void, JSONArray> {
    	
    	protected JSONArray doInBackground(Void...arg0) {			
			String url = "http://messagestore.herokuapp.com/messages";
			HttpResponse response;
			HttpClient httpclient = new DefaultHttpClient();
			String responseString = "";

			try {
	            response = httpclient.execute(new HttpGet(url));
	            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
	                ByteArrayOutputStream out = new ByteArrayOutputStream();
	                response.getEntity().writeTo(out);
	                out.close();
	                responseString = out.toString();
	                Log.d("HI", responseString);
	            } else{
	                //Closes the connection.
	                response.getEntity().getContent().close();
	                throw new IOException(response.getStatusLine().getReasonPhrase());
	            }
	        } catch (ClientProtocolException e) {
	            //TODO Handle problems..
	        } catch (IOException e) {
	            //TODO Handle problems..
	        }
			try {
				JSONArray messages = new JSONArray(responseString);
				return messages;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			return null;
    	}
    
			
		protected void onPostExecute(JSONArray itemsList) {
			data.clear();
			for (int i = 0; i < itemsList.length(); i++) {
				try {
					JSONArray current = itemsList.getJSONArray(i);
					Map<String, String> listItem = new HashMap<String, String>(2);
	                listItem.put("ID", current.getString(0));
					listItem.put("Name", current.getString(1));
	                listItem.put("Comment", current.getString(2));
	                data.add(listItem);
	                
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			adapter.notifyDataSetChanged();
		}
		
    }
    
}
