/*
 * Copyright 2012 GitHub Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.mobile.ui.repo;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.github.mobile.Intents.EXTRA_REPOSITORY;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.github.mobile.Intents.Builder;
import com.github.mobile.R.id;
import com.github.mobile.R.layout;
import com.github.mobile.R.string;
import com.github.mobile.ui.RefreshRepositoryTask;
import com.github.mobile.ui.user.HomeActivity;
import com.github.mobile.util.AvatarLoader;
import com.github.mobile.util.ToastUtils;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;
import com.google.inject.Inject;
import com.viewpagerindicator.TitlePageIndicator;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;

import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;

/**
 * Activity to view a repository
 */
public class RepositoryViewActivity extends RoboSherlockFragmentActivity {

    /**
     * Create intent for this activity
     *
     * @param repository
     * @return intent
     */
    public static Intent createIntent(Repository repository) {
        return new Builder("repo.VIEW").repo(repository).toIntent();
    }

    @InjectExtra(EXTRA_REPOSITORY)
    private Repository repository;

    @Inject
    private AvatarLoader avatarHelper;

    @InjectView(id.vp_pages)
    private ViewPager pager;

    @InjectView(id.pb_loading)
    private ProgressBar loadingBar;

    @InjectView(id.tpi_header)
    private TitlePageIndicator indicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layout.pager_with_title);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(repository.getName());
        actionBar.setSubtitle(repository.getOwner().getLogin());
        actionBar.setDisplayHomeAsUpEnabled(true);

        User owner = repository.getOwner();
        if (owner != null && owner.getAvatarUrl() != null)
            configurePager();
        else {
            loadingBar.setVisibility(VISIBLE);
            pager.setVisibility(GONE);
            indicator.setVisibility(GONE);
            new RefreshRepositoryTask(this, repository) {

                @Override
                protected void onSuccess(Repository fullRepository) throws Exception {
                    super.onSuccess(fullRepository);

                    repository = fullRepository;
                    configurePager();
                }

                @Override
                protected void onException(Exception e) throws RuntimeException {
                    super.onException(e);

                    ToastUtils.show(RepositoryViewActivity.this, string.error_repo_load);
                }
            }.execute();
        }
    }

    private void configurePager() {
        avatarHelper.bind(getSupportActionBar(), repository.getOwner());
        loadingBar.setVisibility(GONE);
        pager.setVisibility(VISIBLE);
        indicator.setVisibility(VISIBLE);
        pager.setAdapter(new RepositoryPagerAdapter(getSupportFragmentManager(), getResources(), repository
                .isHasIssues()));
        indicator.setViewPager(pager);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
