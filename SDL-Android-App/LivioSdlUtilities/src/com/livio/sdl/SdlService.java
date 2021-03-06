package com.livio.sdl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.app.Service;
//import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.livio.sdl.enums.SdlButton;
import com.livio.sdl.menu.CommandButton;
import com.livio.sdl.menu.CommandButton.OnClickListener;
import com.livio.sdl.menu.MenuItem;
import com.livio.sdl.menu.MenuManager;
import com.livio.sdl.menu.SubmenuButton;
import com.livio.sdl.utils.UpCounter;
import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCMessage;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.SdlProxyALM;
import com.smartdevicelink.proxy.interfaces.IProxyListenerALM;
import com.smartdevicelink.proxy.rpc.AddCommand;
import com.smartdevicelink.proxy.rpc.AddCommandResponse;
import com.smartdevicelink.proxy.rpc.AddSubMenu;
import com.smartdevicelink.proxy.rpc.AddSubMenuResponse;
import com.smartdevicelink.proxy.rpc.Alert;
import com.smartdevicelink.proxy.rpc.AlertResponse;
import com.smartdevicelink.proxy.rpc.ChangeRegistrationResponse;
import com.smartdevicelink.proxy.rpc.Choice;
import com.smartdevicelink.proxy.rpc.CreateInteractionChoiceSet;
import com.smartdevicelink.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.smartdevicelink.proxy.rpc.DeleteCommand;
import com.smartdevicelink.proxy.rpc.DeleteCommandResponse;
import com.smartdevicelink.proxy.rpc.DeleteFile;
import com.smartdevicelink.proxy.rpc.DeleteFileResponse;
import com.smartdevicelink.proxy.rpc.DeleteInteractionChoiceSet;
import com.smartdevicelink.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.smartdevicelink.proxy.rpc.DeleteSubMenu;
import com.smartdevicelink.proxy.rpc.DeleteSubMenuResponse;
import com.smartdevicelink.proxy.rpc.DiagnosticMessageResponse;
import com.smartdevicelink.proxy.rpc.DialNumberResponse;
import com.smartdevicelink.proxy.rpc.DisplayTextResponse;
import com.smartdevicelink.proxy.rpc.EndAudioPassThruResponse;
import com.smartdevicelink.proxy.rpc.GenericResponse;
import com.smartdevicelink.proxy.rpc.GetDTCsResponse;
import com.smartdevicelink.proxy.rpc.GetVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.ListFilesResponse;
import com.smartdevicelink.proxy.rpc.OnAudioPassThru;
import com.smartdevicelink.proxy.rpc.OnButtonEvent;
import com.smartdevicelink.proxy.rpc.OnButtonPress;
import com.smartdevicelink.proxy.rpc.OnCommand;
import com.smartdevicelink.proxy.rpc.OnDriverDistraction;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.OnHashChange;
import com.smartdevicelink.proxy.rpc.OnKeyboardInput;
import com.smartdevicelink.proxy.rpc.OnLanguageChange;
import com.smartdevicelink.proxy.rpc.OnLockScreenStatus;
import com.smartdevicelink.proxy.rpc.OnPermissionsChange;
import com.smartdevicelink.proxy.rpc.OnStreamRPC;
import com.smartdevicelink.proxy.rpc.OnSystemRequest;
import com.smartdevicelink.proxy.rpc.OnTBTClientState;
import com.smartdevicelink.proxy.rpc.OnTouchEvent;
import com.smartdevicelink.proxy.rpc.OnVehicleData;
import com.smartdevicelink.proxy.rpc.PerformAudioPassThruResponse;
import com.smartdevicelink.proxy.rpc.PerformInteraction;
import com.smartdevicelink.proxy.rpc.PerformInteractionResponse;
import com.smartdevicelink.proxy.rpc.PutFile;
import com.smartdevicelink.proxy.rpc.PutFileResponse;
import com.smartdevicelink.proxy.rpc.ReadDIDResponse;
import com.smartdevicelink.proxy.rpc.ResetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.ScrollableMessage;
import com.smartdevicelink.proxy.rpc.ScrollableMessageResponse;
import com.smartdevicelink.proxy.rpc.SendLocationResponse;
import com.smartdevicelink.proxy.rpc.SetAppIconResponse;
import com.smartdevicelink.proxy.rpc.SetDisplayLayoutResponse;
import com.smartdevicelink.proxy.rpc.SetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.SetMediaClockTimerResponse;
import com.smartdevicelink.proxy.rpc.Show;
import com.smartdevicelink.proxy.rpc.ShowResponse;
import com.smartdevicelink.proxy.rpc.SliderResponse;
import com.smartdevicelink.proxy.rpc.SoftButton;
import com.smartdevicelink.proxy.rpc.SpeakResponse;
import com.smartdevicelink.proxy.rpc.StreamRPCResponse;
import com.smartdevicelink.proxy.rpc.SubscribeButton;
import com.smartdevicelink.proxy.rpc.SubscribeButtonResponse;
import com.smartdevicelink.proxy.rpc.SubscribeVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.SystemRequestResponse;
import com.smartdevicelink.proxy.rpc.UnsubscribeButton;
import com.smartdevicelink.proxy.rpc.UnsubscribeButtonResponse;
import com.smartdevicelink.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.enums.AppHMIType;
import com.smartdevicelink.proxy.rpc.enums.ButtonName;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.enums.SdlDisconnectedReason;
import com.smartdevicelink.transport.TCPTransportConfig;
import com.smartdevicelink.transport.USBTransportConfig;
//import com.smartdevicelink.transport.USBAccessoryAttachmentActivity;

/**
 * Performs all interactions with Smart Device Link in a long-running service that
 * clients can bind to in order to send information to the vehicle.
 *
 * @author Mike Burke
 *
 */
public  class SdlService extends Service implements IProxyListenerALM
{
	
	/* ********** Nested Classes ********** */
	
	/**
	 * Messages that can be received by a bound client.
	 *
	 * @author Mike Burke
	 *
	 */
	public static class ClientMessages
	{
		/**
		 * Message.what integer called when SDL has successfully created a connection.
		 */
		public static final int SDL_CONNECTED = 0;
		/**
		 * Message.what integer called when SDL has disconnected.
		 */
		public static final int SDL_DISCONNECTED = 1;
		/**
		 * Message.what integer called when the main HMI is first displayed.
		 */
		public static final int SDL_HMI_FIRST_DISPLAYED = 2;
		/**
		 * Message.what integer called when a RPCResponse result has been received.
		 */
		public static final int ON_MESSAGE_RESULT = 3;
		/**
		 * Message.what integer called when a ServiceMessages.REQUEST_SUBMENU_LIST message has been received.
		 */
		public static final int SUBMENU_LIST_RECEIVED = 4;
		/**
		 * Message.what integer called when a ServiceMessages.REQUEST_COMMAND_LIST message has been received.
		 */
		public static final int COMMAND_LIST_RECEIVED = 5;
		/**
		 * Message.what integer called when a ServiceMessages.REQUEST_BUTTON_SUBSCRIPTIONS message has been received.
		 */
		public static final int BUTTON_SUBSCRIPTIONS_RECEIVED = 6;
		/**
		 * Message.what integer called when a ServiceMessages.REQUEST_INTERACTION_SETS message has been received.
		 */
		public static final int INTERACTION_SETS_RECEIVED = 7;
		/**
		 * Message.what integer called when a ServiceMessages.REQUEST_PUT_FILES message has been received.
		 */
		public static final int PUT_FILES_RECEIVED = 8;
	}
	
	/**
	 * Messages that can be sent to the service by a bound client.
	 *
	 * @author Mike Burke
	 *
	 */
	public static class ServiceMessages
	{
		/**
		 * Message.what integer used to register your activity as a client bound to this service.
		 */
		public static final int REGISTER_CLIENT = 0;
		/**
		 * Message.what integer used to unregister your activity as a client bound to this service.
		 */
		public static final int UNREGISTER_CLIENT = 1;
		/**
		 * Message.what integer commanding the service to attempt an SDL connection.
		 */
		public static final int CONNECT = 2;
		/**
		 * Message.what integer commanding the service to disconnect an existing SDL connection.
		 */
		public static final int DISCONNECT = 3;
		/**
		 * Message.what integer commanding the service to reset the SDL connection.
		 */
		public static final int RESET = 4;
		/**
		 * Message.what integer setting the service to Offline mode.
		 */
		public static final int OFFLINE_MODE = 5;
		/**
		 * Message.what integer commanding the service to send an RPCRequest.
		 */
		public static final int SEND_MESSAGE = 6;
		/**
		 * Message.what integer commanding the service to respond with a list of existing submenus that have been added.
		 */
		public static final int REQUEST_SUBMENU_LIST = 7;
		/**
		 * Message.what integer commanding the service to respond with a list of existing commands that have been added.
		 */
		public static final int REQUEST_COMMAND_LIST = 8;
		/**
		 * Message.what integer commanding the service to respond with a list of buttons that have been subscribed to.
		 */
		public static final int REQUEST_BUTTON_SUBSCRIPTIONS = 9;
		/**
		 * Message.what integer commanding the service to respond with a list of interaction sets created so far.
		 */
		public static final int REQUEST_INTERACTION_SETS = 10;
		/**
		 * Message.what integer commanding the service to respond with a list of put file images added so far.
		 */
		public static final int REQUEST_PUT_FILES = 11;
	}
	
	/**
	 * Messages that can be shown on the vehicle head-unit.  Any static
	 * text that your app would like to show on the head-unit can be defined
	 * in this class.
	 *
	 * @author Mike Burke
	 *
	 */
	protected static class MetadataMessages
	{
		public static final String BLANK = " ";
		public static final String APP_NAME = "Livio SDL Tester";
		public static final String APP_SLOGAN = "More Music, Less Work";
	}
	
	/* ********** Static variables ********** */
	protected IpAddress currentIp; // keeps track of the current ip address in case we need to reset
	private static final boolean IS_MEDIA_APP = true;					/*		All of these variables		*/
	private static final Language DEFAULT_LANGUAGE = Language.EN_US;	/*		are needed to start up		*/
	private static final String APP_ID = "2165613258";						/*		the SDL proxy object		*/
	private static final boolean WIFI_AUTO_RECONNECT = true;			/*									*/
	
	protected static boolean debug = false;
	
	/* ********** Instance variables ********** */
	protected List<Messenger> clients = null; // list of bound clients
	
	protected UpCounter correlationIdGenerator = new UpCounter(100); // id generator for correlation ids
	
	protected MenuManager menuManager = new MenuManager();
	protected MenuManager choiceSetManager = new MenuManager();
	protected MenuManager customButtonsManager = new MenuManager();
	protected SdlResponseTracker responseTracker;
	protected List<SdlButton> buttonSubscriptions = new ArrayList<SdlButton>();
	protected List<String> addedImageNames = new ArrayList<String>();
	protected Handler serviceHandler = new Handler();
	
	protected SdlProxyALM sdlProxy = null; // the proxy object which sends our requests and receives responses
	protected boolean isConnected = false;
	protected boolean offlineMode = false;
	protected boolean alreadyDisplayed = false;
	
	protected Toast toast = null;
	
	/* ********** Messenger methods to & from the client ********** */
	
	protected final Messenger messenger = new Messenger(new IncomingHandler());
	
	@SuppressLint("HandlerLeak")
	protected class IncomingHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what)
			{
				case ServiceMessages.REGISTER_CLIENT:
					registerClient(msg.replyTo);
					break;
				case ServiceMessages.UNREGISTER_CLIENT:
					unregisterClient(msg.replyTo);
					break;
				case ServiceMessages.CONNECT:
					offlineMode = false;
					initialize();
					
					// WIFI mode
					if(msg.obj != null){
						IpAddress inputIp = (IpAddress) msg.obj;
						startSdlProxy(inputIp);
					}
					// BT mode
					else{
						startSdlProxy(null);
					}
					break;
				case ServiceMessages.DISCONNECT:
					offlineMode = true;
					initialize();
					stopSdlProxy();
					sendMessageToRegisteredClients(Message.obtain(null, ClientMessages.SDL_DISCONNECTED));
					break;
				case ServiceMessages.RESET:
					initialize();
					resetProxy();
					break;
				case ServiceMessages.OFFLINE_MODE:
					offlineMode = true;
					initialize();
					stopSdlProxy();
					break;
				case ServiceMessages.SEND_MESSAGE:
					onSendMessageReceived((RPCRequest) msg.obj);
					break;
				case ServiceMessages.REQUEST_SUBMENU_LIST:
					submenuListRequested(msg.replyTo, msg.arg1);
					break;
				case ServiceMessages.REQUEST_COMMAND_LIST:
					commandListRequested(msg.replyTo, msg.arg1);
					break;
				case ServiceMessages.REQUEST_BUTTON_SUBSCRIPTIONS:
					buttonSubscriptionsRequested(msg.replyTo, msg.arg1);
					break;
				case ServiceMessages.REQUEST_INTERACTION_SETS:
					interactionSetsRequested(msg.replyTo, msg.arg1);
					break;
				case ServiceMessages.REQUEST_PUT_FILES:
					putFilesRequested(msg.replyTo, msg.arg1);
					break;
				default:
					break;
			}
		}
	}
	
	/**
	 * Registers a client to receive all communication from this service.
	 * 
	 * @param client The client to register
	 */
	protected void registerClient(Messenger client)
	{
		if(clients == null)
		{
			clients = new ArrayList<Messenger>();
		}
		
		clients.add(client);
	}
	
	/**
	 * Removes a client from receiving all communication from this service.
	 * 
	 * @param client The client to remove
	 */
	protected void unregisterClient(Messenger client)
	{
		if(clients != null && clients.size() > 0)
		{
			clients.remove(client);
		}
	}
	
	/**
	 * Sends a message to all registered clients.
	 * 
	 * @param msg The message to send
	 */
	protected void sendMessageToRegisteredClients(Message msg)
	{
		if(clients != null)
		{
			for(Messenger client : clients)
			{
				sendMessageToClient(client, msg);
			}
		}
	}
	
	/**
	 * Sends a message to a single client.
	 * 
	 * @param client The client to reply to
	 * @param msg The message to send
	 */
	protected void sendMessageToClient(Messenger client, Message msg)
	{
		try 
		{
			client.send(msg);
		} catch (RemoteException e) 
		{
			// if we can't send to this client, let's remove it
			unregisterClient(client);
		}
	}
	
	/**
	 * Sends an RPCResponse message to all registered clients.
	 * 
	 * @param response The response to send
	 */
	protected void sendMessageResponse(RPCMessage response)
	{
		Message msg = Message.obtain(null, ClientMessages.ON_MESSAGE_RESULT);
		msg.obj = response;
		sendMessageToRegisteredClients(msg);
	}
	
	/**
	 * Sends the list of available sub-menus to the listening messenger client.
	 * 
	 * @param listener The client to reply to
	 * @param reqCode The request code sent with the initial request
	 */
	protected void submenuListRequested(Messenger listener, int reqCode)
	{
		Message msg = Message.obtain(null, ClientMessages.SUBMENU_LIST_RECEIVED);
		msg.obj = getSubmenuList();
		msg.arg1 = reqCode;
		sendMessageToClient(listener, msg);
	}
	
	/**
	 * Sends the list of available commands to the listening messenger client.
	 * 
	 * @param listener The client to reply to
	 * @param reqCode The request code sent with the initial request
	 */
	protected void commandListRequested(Messenger listener, int reqCode)
	{
		Message msg = Message.obtain(null, ClientMessages.COMMAND_LIST_RECEIVED);
		msg.obj = getCommandList();
		msg.arg1 = reqCode;
		sendMessageToClient(listener, msg);
	}
	
	/**
	 * Sends the list of button subscriptions to the listening messenger client.
	 * 
	 * @param listener The client to reply to
	 * @param reqCode The request code sent with the initial request
	 */
	protected void buttonSubscriptionsRequested(Messenger listener, int reqCode)
	{
		Message msg = Message.obtain(null, ClientMessages.BUTTON_SUBSCRIPTIONS_RECEIVED);
		msg.obj = getButtonSubscriptions();
		msg.arg1 = reqCode;
		sendMessageToClient(listener, msg);
	}
	
	/**
	 * Sends the list of interaction sets to the listening messenger client.
	 * 
	 * @param listener The client to reply to
	 * @param reqCode The request code sent with the initial request
	 */
	protected void interactionSetsRequested(Messenger listener, int reqCode)
	{
		Message msg = Message.obtain(null, ClientMessages.INTERACTION_SETS_RECEIVED);
		msg.obj = getInteractionSets();
		msg.arg1 = reqCode;
		sendMessageToClient(listener, msg);
	}
	
	/**
	 * Sends the list of put files to the listening messenger client.
	 * 
	 * @param listener The client to reply to
	 * @param reqCode The request code sent with the initial request
	 */
	protected void putFilesRequested(Messenger listener, int reqCode)
	{
		Message msg = Message.obtain(null, ClientMessages.PUT_FILES_RECEIVED);
		msg.obj = getPutFiles();
		msg.arg1 = reqCode;
		sendMessageToClient(listener, msg);
	}

	/* ********** Android service life cycle methods ********** */
	@Override
	public void onCreate()
	{
		log("onCreate called");
		initialize();
		super.onCreate();
	}
	
	private void initialize()
	{
		isConnected = false;
		alreadyDisplayed = false;
		
		if(responseTracker == null)
		{
			responseTracker = new SdlResponseTracker(new SdlResponseTracker.Listener() 
			{
				@Override
				public void onRequestTimedOut() 
				{
					if(isConnected && !offlineMode)
					{
						// if any sdl request times out, we will assume we disconnected.
						showToast("A request timed out.  You may need to re-start SDL core.");
						Message msg = Message.obtain(null, ClientMessages.SDL_DISCONNECTED);
						sendMessageToRegisteredClients(msg);
					}
				}
			});
		}
		else
		{
			responseTracker.clear();
		}
		
		correlationIdGenerator.reset();
		IdGenerator.reset();
		
		menuManager.clear();
		choiceSetManager.clear();
		customButtonsManager.clear();
		buttonSubscriptions.clear();
		addedImageNames.clear();
	}

	@Override
	public IBinder onBind(Intent arg0) 
	{
		return messenger.getBinder();
	}
	
	
	/* ********** Proxy life cycle methods ********** */
	/**
	 * Starts up SDL if it isn't already started.  If SDL is already started, this method does nothing.
	 * To reset the proxy, use the resetProxy method.
	 * 
	 * <b>IMPORTANT NOTE</b>
	 * <p>
	 * WiFi connections are for testing purposes only and will not be available in production environments.  WiFi can be used to test
	 * your application on a virtual machine running Ubuntu 12.04, but WiFi should not be used for production testing.  Production-level
	 * testing should be performed on a TDK utilizing a Bluetooth connection.
	 * 
	 * @param inputIp The IP address to attempt a connection on
	 */
	protected void startSdlProxy(final IpAddress inputIp)
	{
		if(sdlProxy == null)
		{
			sdlProxy = createSdlProxyObject(inputIp);
		}
	}
	
	/**
	 * Creates a SmartDeviceLinkProxyALM object and automatically attempts a connection
	 * to the input IP address.
	 * 
	 * <b>IMPORTANT NOTE</b>
	 * <p>
	 * WiFi connections are for testing purposes only and will not be available in production environments.  WiFi can be used to test
	 * your application on a virtual machine running Ubuntu 12.04, but WiFi should not be used for production testing.  Production-level
	 * testing should be performed on a TDK utilizing a Bluetooth connection.
	 * 
	 * @param inputIp The IP address to attempt a connection on
	 * @return The created SmartDeviceLinkProxyALM object
	 */
	protected SdlProxyALM createSdlProxyObject(IpAddress inputIp)
	{
		String appName = getResources().getString(R.string.app_name);
		
		SdlProxyALM result = null;
		try {
			Vector<AppHMIType> test = new Vector<AppHMIType>();
			test.add(AppHMIType.DEFAULT);
			test.add(AppHMIType.MESSAGING);
			test.add(AppHMIType.INFORMATION);
			test.add(AppHMIType.SYSTEM);
			test.add(AppHMIType.TESTING);
			//Context mainactity = new USBAccessoryAttachmentActivity();
			if(inputIp != null)
			{
				result = new SdlProxyALM((IProxyListenerALM)this, null, appName, null, null,
						null, IS_MEDIA_APP, null, DEFAULT_LANGUAGE, DEFAULT_LANGUAGE,test, APP_ID,
						null, false, false, new TCPTransportConfig(Integer.parseInt(inputIp.getTcpPort()), inputIp.getIpAddress(), WIFI_AUTO_RECONNECT));
			}
			else
			{
				result = new SdlProxyALM((IProxyListenerALM)this, null, appName, null, null,
						null, IS_MEDIA_APP, null, DEFAULT_LANGUAGE, DEFAULT_LANGUAGE,test, APP_ID,
						null, false, false,new USBTransportConfig(getApplicationContext()));
			}
			currentIp = inputIp;
		} catch (SdlException e) 
		{
			e.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * Disposes of any current proxy object if it exists and automatically creates a new
	 * proxy connection to the previously connected IP address.
	 */
	protected void resetProxy()
	{
		stopSdlProxy();
		startSdlProxy(currentIp);
	}
	
	/**
	 * Disposes of any current proxy object and sets the object to null so it cannot be
	 * used again.
	 */
	protected void stopSdlProxy()
	{
		if(sdlProxy != null && sdlProxy.getIsConnected() && sdlProxy.getAppInterfaceRegistered()){
			try 
			{
				sdlProxy.dispose();
				sdlProxy = null;
			} catch (SdlException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	/* ********** Proxy communication methods ********** */
	/**
	 * Called when a message to send is received, adds parameters where appropriate (correlation id,
	 * command id, other command-specific parameters, etc).
	 * 
	 * @param command The request to send
	 */
	protected void onSendMessageReceived(RPCRequest command)
	{
		if(command == null)
		{
			throw new NullPointerException("Cannot send a null command.");
		}
		
		if(!offlineMode && sdlProxy == null)
		{
			throw new IllegalStateException("Proxy object is null, so no commands can be sent.");
		}
		
		// set any request-specific parameters if needed
		setRequestSpecificParameters(command);
		
		// after setting appropriate parameters, send the full, completed response back to the clients
		sendMessageResponse(command);
		
		if(!offlineMode)
		{
			// send the request through SmartDeviceLink
			sendRpcRequest(command);
		}
		else
		{
			// in offline mode, we'll just send a "fake" success response.
			SdlResponseFactory.sendGenericResponseForRequest(command, this);
		}
	}
	
	/**
	 * Sends the input request to the connected SDL proxy instance.
	 * 
	 * @param request
	 */
	protected void sendRpcRequest(RPCRequest request)
	{
		try 
		{
			sdlProxy.sendRPCRequest(request);
		} catch (SdlException e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets any command-specific parameters that need to be set.  For example, add command and add submenu commands
	 * need to be assigned an ID at this point.
	 * 
	 * @param command The RPC command to edit
	 */
	protected void setRequestSpecificParameters(RPCRequest command)
	{
		String name = command.getFunctionName();

		// give the command a correlation id
		command.setCorrelationID(correlationIdGenerator.next());
		
		if(name.equals(FunctionID.ADD_COMMAND))
		{
			((AddCommand) command).setCmdID(IdGenerator.next());
			addToRequestQueue(command);
		}
		else if(name.equals(FunctionID.ADD_SUB_MENU))
		{
			((AddSubMenu) command).setMenuID(IdGenerator.next());
			addToRequestQueue(command);
		}
		else if(name.equals(FunctionID.CREATE_INTERACTION_CHOICE_SET))
		{
			CreateInteractionChoiceSet choiceSet = (CreateInteractionChoiceSet) command;
			choiceSet.setInteractionChoiceSetID(IdGenerator.next());

			addToRequestQueue(command);
		}
		else if(name.equals(FunctionID.ALERT))
		{
		    Alert msg = (Alert) command;
		    
		    List<SoftButton> softButtons = msg.getSoftButtons();
		    if(softButtons != null)
		    {
		        for(SoftButton button : softButtons)
		        {
                    customButtonsManager.addItem(createMenuItem(button));
                }
		    }
		    
			int timeout = ((Alert) command).getDuration();
			addToRequestQueue(command, (timeout + SdlConstants.AlertConstants.EXPECTED_REPSONSE_TIME_OFFSET));
		}
		else if(name.equals(FunctionID.PERFORM_INTERACTION))
		{
			int timeout = ((PerformInteraction) command).getTimeout();
			addToRequestQueue(command, (timeout + SdlConstants.PerformInteractionConstants.EXPECTED_REPSONSE_TIME_OFFSET));
		}
		else if(name.equals(FunctionID.SCROLLABLE_MESSAGE))
		{
		    ScrollableMessage msg = (ScrollableMessage) command;
		    
		    List<SoftButton> softButtons = msg.getSoftButtons();
		    if(softButtons != null)
		    {
		        for(SoftButton button : softButtons)
		        {
		            customButtonsManager.addItem(createMenuItem(button));
		        }
		    }
		    
			int timeout = ((ScrollableMessage) command).getTimeout();
			addToRequestQueue(command, (timeout + SdlConstants.ScrollableMessageConstants.EXPECTED_REPSONSE_TIME_OFFSET));
		}
		else if(name.equals(FunctionID.SHOW))
		{
            Show msg = (Show) command;
            
            List<SoftButton> softButtons = msg.getSoftButtons();
            if(softButtons != null)
            {
                for(SoftButton button : softButtons)
                {
                    customButtonsManager.addItem(createMenuItem(button));
                }
            }
		}
		else if( name.equals(FunctionID.PUT_FILE) || name.equals(FunctionID.SUBSCRIBE_BUTTON) || name.equals(FunctionID.SUBSCRIBE_VEHICLE_DATA) ||
				name.equals(FunctionID.UNSUBSCRIBE_VEHICLE_DATA) || name.equals(FunctionID.DELETE_COMMAND) || 
				name.equals(FunctionID.UNSUBSCRIBE_BUTTON) || name.equals(FunctionID.DELETE_INTERACTION_CHOICE_SET) || name.equals(FunctionID.DELETE_SUB_MENU) ||
				name.equals(FunctionID.DELETE_FILE))
		{
			addToRequestQueue(command);
		}
		
	}
	
	/**
	 * Adds the input request to the queue of requests that are awaiting responses.
	 * 
	 * @param request The request to add
	 */
	protected void addToRequestQueue(RPCRequest request)
	{
		responseTracker.add(request);
	}
	
	/**
	 * Adds the input request to the queue of requests that are awaiting responses.
	 * 
	 * @param request The request to add
	 * @param timeout A timeout that exceeds the expected timeout of the request
	 */
	protected void addToRequestQueue(RPCRequest request, int timeout)
	{
		responseTracker.add(request, timeout);
	}
	
	/**
	 * Removes the input request from the queue of requests that are awaiting responses.
	 * 
	 * @param request The request to remove
	 */
	protected RPCRequest removeFromRequestQueue(int key)
	{
		return responseTracker.remove(key);
	}
	
	/**
	 * Translates the AddCommand object into a MenuItem object, complete with a click listener.
	 * 
	 * @param command The command to translate
	 * @return The translated MenuItem object
	 */
	protected MenuItem createMenuItem(AddCommand command)
	{
		final String name = command.getMenuParams().getMenuName();
		final int id = command.getCmdID();
		int parentId;
		final Integer parentInteger = command.getMenuParams().getParentID();
		if(parentInteger == null)
		{
			parentId = -1;
		}
		else
		{
			parentId = parentInteger;
		}
		
		final MenuItem result = new CommandButton(name, id, parentId, new OnClickListener()
		{
			@Override
			public void onClick(CommandButton button) 
			{
				showToast(new StringBuilder().append(name).append(" clicked!").toString());
			}
		});
		
		return result;
	}
	
	/**
	 * Translates the AddSubMenu object into a MenuItem object.
	 * 
	 * @param command The command to translate
	 * @return The translated MenuItem object
	 */
	protected MenuItem createMenuItem(AddSubMenu command)
	{
		final String name = command.getMenuName();
		final MenuItem result = new SubmenuButton(name, command.getMenuID());
		return result;
	}
	
	/**
	 * Translates the CreateInteractionChoiceSet object into a MenuItem object.
	 * 
	 * @param command The command to translate
	 * @return The translated MenuItem object
	 */
	protected MenuItem createMenuItem(CreateInteractionChoiceSet command){
		final String name = "Choice Set";
		final MenuItem result = new SubmenuButton(name, command.getInteractionChoiceSetID());
		return result;
	}
	
	/**
	 * Translates the CreateInteractionChoiceSet object into a MenuItem object, complete with a click listener.
	 * 
	 * @param choice The command to translate
	 * @param parentId The parent id of the input choice command
	 * @return The translated MenuItem object
	 */
	protected MenuItem createMenuItem(Choice choice, final int parentId){
		final String name = choice.getMenuName();
		final int id = choice.getChoiceID();
		final MenuItem result = new CommandButton(name, id, parentId, new OnClickListener(){
			@Override
			public void onClick(CommandButton button) {
				showToast(new StringBuilder().append(name).append(" clicked!").toString());
			}
		});
		
		return result;
	}
    
    protected MenuItem createMenuItem(SoftButton button){
        final String name = button.getText();
        final int id = button.getSoftButtonID();
        final MenuItem result = new CommandButton(name, id, SdlConstants.AddCommandConstants.INVALID_PARENT_ID, new OnClickListener(){
            @Override
            public void onClick(CommandButton button){
                showToast(new StringBuilder().append(name).append(" clicked!").toString());
            }
        });
        
        return result;
    }
	
	/**
	 * Creates a copy of the list of submenus added so far.
	 * 
	 * @return The copied list of submenu items
	 */
	protected List<MenuItem> getSubmenuList(){
		return menuManager.getSubmenus();
	}
	
	/**
	 * Creates a copy of the list of commands added so far.
	 * 
	 * @return The copied list of command items
	 */
	protected List<MenuItem> getCommandList(){
		return menuManager.getCommands();
	}
	
	/**
	 * Creates a copy of the choice set menus added so far.
	 * 
	 * @return The copied list of choice set items
	 */
	protected List<MenuItem> getChoiceSetList(){
		return choiceSetManager.getSubmenus();
	}
	
	/**
	 * Creates a copy of the list of button subscriptions added so far.
	 * 
	 * @return The copied list of button subscriptions
	 */
	protected List<SdlButton> getButtonSubscriptions(){
		if(buttonSubscriptions == null || buttonSubscriptions.size() <= 0){
			return Collections.emptyList();
		}
		
		return new ArrayList<SdlButton>(buttonSubscriptions);
	}
	
	/**
	 * Creates a copy of the list of interaction sets added so far.
	 * 
	 * @return The copied list of interaction sets
	 */
	protected List<MenuItem> getInteractionSets(){
		List<MenuItem> result = choiceSetManager.getSubmenus();
		if(result == null || result.size() <= 0){
			return Collections.emptyList();
		}
		
		return result;
	}
	
	/**
	 * Creates a copy of the list of image names added so far.
	 * 
	 * @return The copied list of image names
	 */
	protected List<String> getPutFiles(){
		if(addedImageNames == null || addedImageNames.size() <= 0){
			return Collections.emptyList();
		}
		
		return new ArrayList<String>(addedImageNames);
	}
	
	/**
	 * Posts the input runnable to the Service thread.
	 * 
	 * @param runnable The runnable to run
	 */
	protected void runOnServiceThread(Runnable runnable){
		serviceHandler.post(runnable);
	}

	/* ********** IProxyListenerALM interface methods ********** */
	
	/* Most useful callbacks */
	@Override
	public void onOnHMIStatus(OnHMIStatus newStatus) {
		if(!isConnected){
			Message msg = Message.obtain(null, ClientMessages.SDL_CONNECTED);
			sendMessageToRegisteredClients(msg);
			isConnected = true;
			offlineMode = false;
		}
		
		if(newStatus.getHmiLevel() == HMILevel.HMI_FULL && !alreadyDisplayed){
			Message msg = Message.obtain(null, ClientMessages.SDL_HMI_FIRST_DISPLAYED);
			sendMessageToRegisteredClients(msg);
			alreadyDisplayed = true;
		}
		
		sendMessageResponse(newStatus);
	}
	
	@Override 
	public void onOnCommand(final OnCommand notification) {
		runOnServiceThread(new Runnable() {
			@Override
			public void run() {
				sendMessageResponse(notification);
				
				int buttonId = notification.getCmdID();
				menuManager.dispatchClick(buttonId);
			}
		});
	}
	
	@Override
	public void onOnButtonPress(final OnButtonPress notification) {
		runOnServiceThread(new Runnable() {
			@Override
			public void run() {
				sendMessageResponse(notification);
				
				ButtonName button = notification.getButtonName();
				SdlButton sdlButton = SdlButton.translateFromLegacy(button);
				if(sdlButton == SdlButton.CUSTOM_BUTTON){
				    customButtonsManager.dispatchClick(notification.getCustomButtonName());
				}
				else{
    				String text = new StringBuilder().append(sdlButton.toString()).append(" clicked!").toString();
    				showToast(text);
				}
			}
		});
	}
	
	/* Not very useful callbacks */
	@Override public void onOnPermissionsChange(OnPermissionsChange notification) {sendMessageResponse(notification);}
	@Override public void onOnVehicleData(OnVehicleData notification) {sendMessageResponse(notification);}
	@Override public void onOnAudioPassThru(OnAudioPassThru notification) {sendMessageResponse(notification);}
	@Override public void onOnLanguageChange(OnLanguageChange notification) {sendMessageResponse(notification);}
	@Override public void onOnDriverDistraction(OnDriverDistraction notification) {sendMessageResponse(notification);}
	@Override public void onOnTBTClientState(OnTBTClientState notification) {sendMessageResponse(notification);}
	@Override public void onError(String info, Exception e) {}
	@Override public void onOnButtonEvent(OnButtonEvent notification) {sendMessageResponse(notification);}
	
	/* Message responses */
	@Override
	public void onAddCommandResponse(AddCommandResponse response) {
		sendMessageResponse(response);

		int correlationId = response.getCorrelationID();
		RPCRequest original = removeFromRequestQueue(correlationId);
		
		if(response.getSuccess() && original != null){
			MenuItem button = createMenuItem((AddCommand) original);
			if(button != null){
				menuManager.addItem(button);
			}
		}
	}
	
	@Override
	public void onDeleteCommandResponse(DeleteCommandResponse response) {
		sendMessageResponse(response);

		int correlationId = response.getCorrelationID();
		RPCRequest original = removeFromRequestQueue(correlationId);
		
		if(response.getSuccess() && original != null){
			if(response.getSuccess()){
				int idToRemove = ((DeleteCommand) original).getCmdID();
				menuManager.removeItem(idToRemove);
			}
		}
	}
	
	@Override
	public void onAddSubMenuResponse(AddSubMenuResponse response) {
		sendMessageResponse(response);

		int correlationId = response.getCorrelationID();
		RPCRequest original = removeFromRequestQueue(correlationId);
		
		if(response.getSuccess() && original != null){
			MenuItem button = createMenuItem((AddSubMenu) original);
			if(button != null){
				menuManager.addItem(button);
			}
		}
	}
	
	@Override
	public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {
		sendMessageResponse(response);

		int correlationId = response.getCorrelationID();
		RPCRequest original = removeFromRequestQueue(correlationId);
		
		if(response.getSuccess() && original != null){
			int idToRemove = ((DeleteSubMenu) original).getMenuID();
			menuManager.removeItem(idToRemove);
		}
	}
	@Override
	public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
		sendMessageResponse(response);

		int correlationId = response.getCorrelationID();
		RPCRequest original = removeFromRequestQueue(correlationId);
		
		if(response.getSuccess() && original != null){
			SdlButton button = SdlButton.translateFromLegacy(((SubscribeButton) original).getButtonName());
			buttonSubscriptions.add(button);
		}
	}
	
	@Override
	public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
		sendMessageResponse(response);

		int correlationId = response.getCorrelationID();
		RPCRequest original = removeFromRequestQueue(correlationId);
		
		if(response.getSuccess() && original != null){
			SdlButton button = SdlButton.translateFromLegacy(((UnsubscribeButton) original).getButtonName());
			buttonSubscriptions.remove(button);
		}
	}

	@Override 
	public void onCreateInteractionChoiceSetResponse(CreateInteractionChoiceSetResponse response) {
		sendMessageResponse(response);

		int correlationId = response.getCorrelationID();
		RPCRequest original = removeFromRequestQueue(correlationId);
		
		if(response.getSuccess() && original != null){
			// add the parent (choice set) item to the choice set manager
			CreateInteractionChoiceSet choiceSet = (CreateInteractionChoiceSet) original;
			MenuItem item = createMenuItem(choiceSet);
			choiceSetManager.addItem(item);
			
			// then, add all the parent's children to the choice set manager
			final int parentId = choiceSet.getInteractionChoiceSetID();
			List<Choice> children = choiceSet.getChoiceSet();
			for(Choice child : children){
				item = createMenuItem(child, parentId);
				choiceSetManager.addItem(item);
			}
		}
	}

	@Override 
	public void onDeleteInteractionChoiceSetResponse(DeleteInteractionChoiceSetResponse response) {
		sendMessageResponse(response);

		int correlationId = response.getCorrelationID();
		RPCRequest original = removeFromRequestQueue(correlationId);
		
		if(response.getSuccess() && original != null){
			// get the choice set ID from the original request and remove it from the choice set manager
			DeleteInteractionChoiceSet choiceSet = (DeleteInteractionChoiceSet) original;
			int choiceId = choiceSet.getInteractionChoiceSetID();
			choiceSetManager.removeItem(choiceId);
		}
	}
	
	@Override 
	public void onPutFileResponse(PutFileResponse response) {
		sendMessageResponse(response);

		int correlationId = response.getCorrelationID();
		RPCRequest original = removeFromRequestQueue(correlationId);
		
		if(response.getSuccess() && original != null){
			// get the choice set ID from the original request and remove it from the choice set manager
			PutFile putFile = (PutFile) original;
			String putFileName = putFile.getSdlFileName();
			addedImageNames.add(putFileName);
		}
	}

	@Override 
	public void onDeleteFileResponse(DeleteFileResponse response) {
		sendMessageResponse(response);

		int correlationId = response.getCorrelationID();
		RPCRequest original = removeFromRequestQueue(correlationId);
		
		if(response.getSuccess() && original != null){
			// get the choice set ID from the original request and remove it from the choice set manager
			DeleteFile deleteFile = (DeleteFile) original;
			String deleteFileName = deleteFile.getSdlFileName();
			addedImageNames.remove(deleteFileName);
		}
	}
	@Override
	public void onAlertResponse(AlertResponse response) {
		sendMessageResponse(response);
		
		int correlationId = response.getCorrelationID();
		removeFromRequestQueue(correlationId);
	}
	
	@Override 
	public void onPerformInteractionResponse(final PerformInteractionResponse response) {
		runOnServiceThread(new Runnable() {
			@Override
			public void run() {
				sendMessageResponse(response);
				
				int correlationId = response.getCorrelationID();
				removeFromRequestQueue(correlationId);
				
				if(response.getSuccess()){
					Integer choiceId = response.getChoiceID();
					if(choiceId != null){
						int interactionId = choiceId; // auto-unbox the Integer object if it's not null
						choiceSetManager.dispatchClick(interactionId);
					}
				}
			}
		});
	}

	@Override 
	public void onScrollableMessageResponse(ScrollableMessageResponse response) {
		sendMessageResponse(response);
		
		int correlationId = response.getCorrelationID();
		removeFromRequestQueue(correlationId);
	}

    @Override public void onSliderResponse(SliderResponse response) {sendMessageResponse(response);}
	@Override public void onSubscribeVehicleDataResponse(SubscribeVehicleDataResponse response) {sendMessageResponse(response);}
	@Override public void onUnsubscribeVehicleDataResponse(UnsubscribeVehicleDataResponse response) {sendMessageResponse(response);}
	@Override public void onGenericResponse(GenericResponse response) {sendMessageResponse(response);}
	@Override public void onResetGlobalPropertiesResponse(ResetGlobalPropertiesResponse response) {sendMessageResponse(response);}
	@Override public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse response) {sendMessageResponse(response);}
	@Override public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {sendMessageResponse(response);}
	@Override public void onShowResponse(ShowResponse response) {sendMessageResponse(response);}
	@Override public void onSpeakResponse(SpeakResponse response) {sendMessageResponse(response);}
	@Override public void onGetVehicleDataResponse(GetVehicleDataResponse response) {sendMessageResponse(response);}
	@Override public void onReadDIDResponse(ReadDIDResponse response) {sendMessageResponse(response);}
	@Override public void onGetDTCsResponse(GetDTCsResponse response) {sendMessageResponse(response);}
	@Override public void onPerformAudioPassThruResponse(PerformAudioPassThruResponse response) {sendMessageResponse(response);}
	@Override public void onEndAudioPassThruResponse(EndAudioPassThruResponse response) {sendMessageResponse(response);}
	@Override public void onListFilesResponse(ListFilesResponse response) {sendMessageResponse(response);}
	@Override public void onSetAppIconResponse(SetAppIconResponse response) {sendMessageResponse(response);}
	@Override public void onChangeRegistrationResponse(ChangeRegistrationResponse response) {sendMessageResponse(response);}
	@Override public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse response) {sendMessageResponse(response);}
	
	//huaguoping-add-20150908-start
	@Override public void onDisplayTextResponse(DisplayTextResponse response) {sendMessageResponse(response);}
	//huaguoping-add-20150908-end
	
	private void showToast(String msg){
		if(toast == null){
			toast = Toast.makeText(SdlService.this, "", Toast.LENGTH_LONG);
		}
		
		toast.setText(msg);
		toast.show();
	}

	/* ********** Debug & log methods ********** */
	/**
	 * Enables debug mode for this class and any classes used in this class.
	 * 
	 * @param enable Enable flag for debug mode
	 */
	public static void setDebug(boolean enable){
		debug = enable;
		MenuManager.setDebug(enable);
	}
	
	private static void log(String msg){
		if(debug){
			Log.d("SdlService", msg);
		}
	}

    @Override
    public void onProxyClosed(String info, Exception e, SdlDisconnectedReason reason){
        Message msg = Message.obtain(null, ClientMessages.SDL_DISCONNECTED);
        sendMessageToRegisteredClients(msg);
        stopSdlProxy();
        initialize();
    }

    @Override
    public void onOnHashChange(OnHashChange notification){
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onOnSystemRequest(OnSystemRequest notification){
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onSystemRequestResponse(SystemRequestResponse response){
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onOnKeyboardInput(OnKeyboardInput notification){
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onOnTouchEvent(OnTouchEvent notification){
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onDiagnosticMessageResponse(DiagnosticMessageResponse response){
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onOnLockScreenNotification(OnLockScreenStatus notification){
        // TODO Auto-generated method stub
        
    }

	@Override
	public void onOnStreamRPC(OnStreamRPC notification) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStreamRPCResponse(StreamRPCResponse response) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialNumberResponse(DialNumberResponse response) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSendLocationResponse(SendLocationResponse response) {
		// TODO Auto-generated method stub
		
	}
}
