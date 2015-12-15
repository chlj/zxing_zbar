package com.example.zxing_zbar_eclipse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.zxing_zbar.R;

public class MainActivity extends Activity {
	private ImageButton btn_home_scan;
	private EditText et_mailno_search;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		et_mailno_search = (EditText) this.findViewById(R.id.et_mailno_search);
		btn_home_scan = (ImageButton) this.findViewById(R.id.btn_home_scan);
		btn_home_scan.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				et_mailno_search.setText("");
				Intent intent = new Intent(MainActivity.this,
						ScannerQRCodeActivity.class);
				startActivityForResult(intent, 0);
			}
		});

	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);
		if(data!=null){
			if (resultCode == RESULT_OK) 
			{
				Bundle bundle = data.getExtras();
				String scanResult = bundle.getString("result");
				et_mailno_search.setText(scanResult);
				et_mailno_search.setSelection(et_mailno_search.length());
			}
		}
		
	}
	

}
