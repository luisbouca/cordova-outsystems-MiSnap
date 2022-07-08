import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.outsystems.misnap.MiSnap;
import android.content.Intent;

var public ActivityResultLauncher<Intent> misnapActivityResultLauncher;

function public void onCreate(Bundle savedInstanceState)
    MainActivity main = this;
    misnapActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    MiSnap misnap = (MiSnap) main.appView.getPluginManager().getPlugin("MiSnap");
                    misnap.callbackResult(result);
                }
            });
end function