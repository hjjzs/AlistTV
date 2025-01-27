package com.hjj.tvalist;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.leanback.widget.VerticalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import android.widget.TextView;
import com.hjj.tvalist.api.ApiClient;
import com.hjj.tvalist.api.AlistService;
import com.hjj.tvalist.model.AlistResponse;
import com.hjj.tvalist.presenter.FileItemPresenter;
import com.hjj.tvalist.util.FileUtils;
import com.hjj.tvalist.util.SettingsManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
import android.content.Intent;
import android.net.Uri;
import android.view.KeyEvent;
import com.hjj.tvalist.model.MenuItem;
import com.hjj.tvalist.presenter.MenuItemPresenter;
import androidx.recyclerview.widget.RecyclerView;


public class MainActivity extends FragmentActivity {
    private static final String TAG = "MainActivity";
    private TextView titleView;
    private VerticalGridView gridView;
    private ArrayObjectAdapter adapter;
    private AlistService alistService;
    private String currentPath = "/";
    private Stack<String> pathHistory = new Stack<>();
    private Map<String, Integer> pathPositionMap = new HashMap<>();
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    private ProgressBar loadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        titleView = findViewById(R.id.title_view);
        gridView = findViewById(R.id.grid);
        loadingIndicator = findViewById(R.id.loading_indicator);

        setupUI();
        setupGrid();
        login();  // 先登录，再加载内容
    }

    private void setupUI() {
        titleView.setText(getString(R.string.app_name));
        alistService = ApiClient.getClient().create(AlistService.class);

        // 添加返回键处理
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!pathHistory.isEmpty()) {
                    navigateUp();
                } else {
                    finish();
                }
            }
        });

        // 设置菜单
        setupMenu();

        // 初始化 FileUtils
        FileUtils.init(this);
    }

    private void setupMenu() {
        VerticalGridView menuGrid = findViewById(R.id.menu_grid);
        ArrayObjectAdapter menuAdapter = new ArrayObjectAdapter(new MenuItemPresenter());

        // 添加菜单项
        menuAdapter.add(new MenuItem("设置", R.drawable.ic_settings, () -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }));
        // 刷新目录
        menuAdapter.add(new MenuItem("刷新", R.drawable.refresh, () -> {
            showLoading();
            loadContent(currentPath, true);
        }));
        // 可以在这里添加更多菜单项
        menuAdapter.add(new MenuItem("关于", R.drawable.about, () -> {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }));
        // 检查更新
        menuAdapter.add(new MenuItem("检查更新", R.drawable.update, () -> {
            Intent intent = new Intent(this, CheckForUpdatesActivity.class);
            startActivity(intent);
        }));

        // 退出
        menuAdapter.add(new MenuItem("退出", R.drawable.exit, () -> {
            finish();
        }));

        menuGrid.setAdapter(new ItemBridgeAdapter(menuAdapter));

        // 添加网格视图的焦点监听
        gridView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    // 当在最右边的项目按右键时，焦点转移到菜单
                    int position = gridView.getSelectedPosition();
                    int columns = 3; // 网格的列数
                    if ((position + 1) % columns == 0) {
                        menuGrid.requestFocus();
                        return true;
                    }
                }
            }
            return false;
        });

        // 添加菜单的焦点监听
        menuGrid.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    // 从菜单按左键时，焦点返回到网格
                    gridView.requestFocus();
                    return true;
                }
            }
            return false;
        });
    }

    private void refreshContent() {
        showLoading();

    }

    private void setupGrid() {
        VerticalGridPresenter presenter = new VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_LARGE);
        presenter.setNumberOfColumns(3);

        FileItemPresenter fileItemPresenter = new FileItemPresenter();
        fileItemPresenter.setOnItemClickListener(this::onItemClick);

        adapter = new ArrayObjectAdapter(fileItemPresenter);
        ItemBridgeAdapter bridgeAdapter = new ItemBridgeAdapter(adapter);
        gridView.setAdapter(bridgeAdapter);

        // 修改滚动监听，避免类型转换错误
        gridView.setOnScrollListener(new VerticalGridView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // 当停止滚动时
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // 获取最后一个可见项的位置
                    int lastVisibleItem = gridView.getChildCount() > 0 ? gridView.getSelectedPosition() : -1;
                    int totalItemCount = adapter.size();

                    // 当滚动到倒数第6个项目时，加载更多
                    if (lastVisibleItem >= totalItemCount - 6 && !isLoading && hasMoreData) {
                        loadMoreContent();
                    }
                }
            }
        });
    }

    private void showLoading() {
        loadingIndicator.setVisibility(View.VISIBLE);
        gridView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
        gridView.setVisibility(View.VISIBLE);
    }

    private void onItemClick(AlistResponse.Content content) {
        if (content.is_dir) {
            // 点击目录时显示加载动画
            showLoading();

            // 先保存当前路径，等确认新目录不为空时再保存位置
            String newPath = currentPath.equals("/") ?
                    "/" + content.name :
                    currentPath + "/" + content.name;

            // 检查新目录内容
            checkAndLoadDirectory(newPath, gridView.getSelectedPosition());
        } else if (FileUtils.isVideoFile(content.name)) {
            playVideoWithMXPlayer(content);
        }
    }

    private void checkAndLoadDirectory(String newPath, int currentPosition) {
        // 重置分页状态
        currentPage = 1;
        hasMoreData = true;

        AlistService.ListRequest request = new AlistService.ListRequest(newPath);
        request.page = currentPage;
        alistService.listFiles(request).enqueue(new Callback<AlistResponse>() {
            @Override
            public void onResponse(Call<AlistResponse> call, Response<AlistResponse> response) {
                if (response.isSuccessful()) {
                    AlistResponse alistResponse = response.body();
                    if (alistResponse != null && alistResponse.code == 200 &&
                            alistResponse.data != null && alistResponse.data.content != null &&
                            !alistResponse.data.content.isEmpty()) {

                        // 目录不为空，保存当前路径和位置
                        pathHistory.push(currentPath);
                        pathPositionMap.put(currentPath, currentPosition);
                        currentPath = newPath;

                        // 更新UI
                        adapter.clear();
                        adapter.addAll(0, alistResponse.data.content);
                        updateTitle();


                        // 设置焦点到第一个位置
                        runOnUiThread(() -> {
                            if (gridView.getChildCount() > 0) {
                                gridView.setSelectedPosition(0);
                            }
                        });

                        // 在这里添加将焦点设置回 gridView 的代码
                        runOnUiThread(() -> {
                            gridView.requestFocus(); // 将焦点设置回列表
                        });
                    } else {
                        showError("目录为空");
                        gridView.requestFocus(); // 将焦点设置回列表
                    }
                } else {
                    showError("加载失败: " + response.code());
                }
                hideLoading();
            }

            @Override
            public void onFailure(Call<AlistResponse> call, Throwable t) {
                Log.e(TAG, "Request failed", t);
                showError("网络请求失败: " + t.getMessage());
                hideLoading();
            }
        });
    }

    private void navigateUp() {
        if (!pathHistory.isEmpty()) {
            currentPath = pathHistory.pop();
            updateTitle();
            // 传入要恢复的位置
            Integer position = pathPositionMap.get(currentPath);
            loadContent(currentPath, position, false);
            // 清理不需要的位置记录
            pathPositionMap.remove(currentPath);
        }
    }

    private void updateTitle() {
        String displayPath = currentPath.equals("/") ? "根目录" : currentPath;
        titleView.setText(displayPath);
    }

    private void login() {
        SettingsManager settingsManager = new SettingsManager(this);
        String username = settingsManager.getUsername();
        String password = settingsManager.getPassword();

        AlistService.LoginRequest loginRequest = new AlistService.LoginRequest(username, password);
        alistService.login(loginRequest).enqueue(new Callback<AlistResponse>() {
            @Override
            public void onResponse(Call<AlistResponse> call, Response<AlistResponse> response) {
                if (response.isSuccessful()) {
                    AlistResponse loginResponse = response.body();
                    if (loginResponse != null) {
                        if (loginResponse.code == 200 && loginResponse.data != null) {
                            String token = loginResponse.data.token;
                            if (token != null && !token.isEmpty()) {
                                ApiClient.setToken(token);
                                loadContent(currentPath, false);
                            } else {
                                Log.e(TAG, "No token in response data");
                                showError("登录失败：未获取到认证信息");
                            }
                        } else {
                            Log.e(TAG, "Login failed: " + loginResponse.message);
                            showError("登录失败：" + loginResponse.message);
                        }
                    } else {
                        Log.e(TAG, "Login response body is null");
                        showError("登录失败：返回数据为空");
                    }
                } else {
                    Log.e(TAG, "Login not successful: " + response.code());
                    showError("登录失败：" + response.code());
                }
            }

            @Override
            public void onFailure(Call<AlistResponse> call, Throwable t) {
                Log.e(TAG, "Login request failed", t);
                showError("登录请求失败：" + t.getMessage());
            }
        });
    }

    private void loadContent(String path, boolean refresh) {
        loadContent(path, 0, refresh);
    }

    private void loadContent(String path, Integer position, boolean refresh) {
        // 重置分页状态
        currentPage = 1;
        hasMoreData = true;

        AlistService.ListRequest request = new AlistService.ListRequest(path);
        request.page = currentPage;
        request.refresh = refresh;
        alistService.listFiles(request).enqueue(new Callback<AlistResponse>() {
            @Override
            public void onResponse(Call<AlistResponse> call, Response<AlistResponse> response) {
                if (response.isSuccessful()) {
                    AlistResponse alistResponse = response.body();
                    if (alistResponse != null) {
                        if (alistResponse.code == 200) {
                            if (alistResponse.data != null && alistResponse.data.content != null) {
                                adapter.clear();
                                adapter.addAll(0, alistResponse.data.content);
                                updateTitle();

                                // 数据加载完成后，将焦点设置到指定位置
                                runOnUiThread(() -> {
                                    if (gridView.getChildCount() > 0) {
                                        // 确保position不超过列表大小
                                        int targetPosition = Math.min(position != null ? position : 0,
                                                adapter.size() - 1);
                                        gridView.setSelectedPosition(targetPosition);
                                    }
                                });

                            } else {
                                Log.e(TAG, "Response data or content is null");
                                showError("目录为空");
                            }
                        } else {
                            Log.e(TAG, "Response code is not 200: " + alistResponse.code);
                            showError("加载失败：" + alistResponse.message);
                        }
                    } else {
                        Log.e(TAG, "Response body is null");
                        showError("返回数据为空");
                    }
                } else {
                    Log.e(TAG, "Response not successful: " + response.code());
                    showError("请求失败: " + response.code());
                }
                hideLoading();
            }

            @Override
            public void onFailure(Call<AlistResponse> call, Throwable t) {
                Log.e(TAG, "Request failed", t);
                showError("网络请求失败: " + t.getMessage());
                hideLoading();
            }
        });
    }

    private void playVideoWithMXPlayer(AlistResponse.Content content) {
        // 构建请求体
        AlistService.GetRequest request = new AlistService.GetRequest(
                currentPath.equals("/") ? "/" + content.name : currentPath + "/" + content.name
        );

        // 发起请求获取真实播放地址
        alistService.getFile(request).enqueue(new Callback<AlistResponse>() {
            @Override
            public void onResponse(Call<AlistResponse> call, Response<AlistResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AlistResponse getResponse = response.body();
                    if (getResponse.code == 200 && getResponse.data != null && getResponse.data.raw_url != null) {
                        // 获取到真实播放地址后启动MX Player
                        playWithMXPlayer(getResponse.data.raw_url);
                    } else {
                        showError("获取播放地址失败：" + getResponse.message);
                    }
                } else {
                    showError("获取播放地址失败：" + response.code());
                }
            }

            @Override
            public void onFailure(Call<AlistResponse> call, Throwable t) {
                showError("获取播放地址请求失败：" + t.getMessage());
            }
        });
    }

    private void playWithMXPlayer(String videoUrl) {
        // 创建播放意图
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(videoUrl), "video/*");

        // 尝试使用MX Player Pro
        intent.setPackage("com.mxtech.videoplayer.pro");

        try {
            startActivity(intent);
        } catch (Exception e) {
            // 如果没有安装Pro版本，尝试使用免费版
            try {
                intent.setPackage("com.mxtech.videoplayer.ad");
                startActivity(intent);
            } catch (Exception e1) {
                // 如果都没有安装，提示用户安装MX Player
                showError("请安装MX Player播放器");
            }
        }
    }

    private void showError(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void loadMoreContent() {
        if (isLoading) return;

        isLoading = true;
        currentPage++;

        AlistService.ListRequest request = new AlistService.ListRequest(currentPath);
        request.page = currentPage;

        alistService.listFiles(request).enqueue(new Callback<AlistResponse>() {
            @Override
            public void onResponse(Call<AlistResponse> call, Response<AlistResponse> response) {
                if (response.isSuccessful()) {
                    AlistResponse alistResponse = response.body();
                    if (alistResponse != null && alistResponse.code == 200 &&
                            alistResponse.data != null && alistResponse.data.content != null) {

                        if (!alistResponse.data.content.isEmpty()) {
                            // 添加新数据到adapter
                            adapter.addAll(adapter.size(), alistResponse.data.content);
                        } else {
                            // 没有更多数据了
                            hasMoreData = false;
                        }
                    }
                }
                isLoading = false;
            }

            @Override
            public void onFailure(Call<AlistResponse> call, Throwable t) {
                Log.e(TAG, "Load more failed", t);
                isLoading = false;
                currentPage--; // 失败时恢复页码
            }
        });
    }

} 