/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.storage.indices;

import static com.exonum.binding.hash.Hashing.DEFAULT_HASH_SIZE_BITS;
import static com.exonum.binding.hash.Hashing.DEFAULT_HASH_SIZE_BYTES;
import static com.exonum.binding.storage.indices.ProofListContainsMatcher.provesThatContains;
import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.util.LibraryLoader;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Contains tests of ProofListIndexProxy methods
 * that are not present in {@link ListIndex} interface.
 */
public class ProofListIndexProxyIntegrationTest {

  /**
   * An empty list root hash: an all-zero hash code.
   */
  private static final HashCode EMPTY_LIST_ROOT_HASH =
      HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);

  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Database database;

  private static final String LIST_NAME = "test_proof_list";

  @Before
  public void setUp() {
    database = MemoryDb.newInstance();
  }

  @After
  public void tearDown() {
    database.close();
  }

  @Test
  public void getRootHashEmptyList() {
    runTestWithView(database::createSnapshot, (list) -> {
      assertThat(list.getRootHash(), equalTo(EMPTY_LIST_ROOT_HASH));
    });
  }

  @Test
  public void getRootHashSingletonList() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      HashCode rootHash = list.getRootHash();
      assertThat(rootHash.bits(), equalTo(DEFAULT_HASH_SIZE_BITS));
      assertThat(rootHash, not(equalTo(EMPTY_LIST_ROOT_HASH)));
    });
  }

  @Test
  public void getProofFailsIfEmptyList() {
    runTestWithView(database::createSnapshot, (list) -> {
      expectedException.expect(IndexOutOfBoundsException.class);
      list.getProof(0);
    });
  }

  @Test
  public void getProofSingletonList() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      assertThat(list, provesThatContains(0, V1));
    });
  }

  @Test
  public void getRangeProofSingletonList() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      assertThat(list, provesThatContains(0, singletonList(V1)));
    });
  }

  @Test
  public void getProofMultipleItemList() {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;

      list.addAll(values);

      for (int i = 0; i < values.size(); i++) {
        assertThat(list, provesThatContains(i, values.get(i)));
      }
    });
  }

  @Test
  public void getRangeProofMultipleItemList_FullRange() {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;
      list.addAll(values);

      assertThat(list, provesThatContains(0, values));
    });
  }

  @Test
  public void getRangeProofMultipleItemList_1stHalf() {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;
      list.addAll(values);

      int from = 0;
      int to = values.size() / 2;
      assertThat(list, provesThatContains(from, values.subList(from, to)));
    });
  }

  @Test
  public void getRangeProofMultipleItemList_2ndHalf() {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;
      list.addAll(values);

      int from = values.size() / 2;
      int to = values.size();
      assertThat(list, provesThatContains(from, values.subList(from, to)));
    });
  }

  private static void runTestWithView(Function<Cleaner, View> viewFactory,
                               Consumer<ProofListIndexProxy<String>> listTest) {
    runTestWithView(viewFactory, (ignoredView, list) -> listTest.accept(list));
  }

  private static void runTestWithView(Function<Cleaner, View> viewFactory,
                               BiConsumer<View, ProofListIndexProxy<String>> listTest) {
    IndicesTests.runTestWithView(
        viewFactory,
        LIST_NAME,
        ProofListIndexProxy::newInstance,
        listTest
    );
  }
}
