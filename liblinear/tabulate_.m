function [] = testNgramModel
clear;
javaaddpath('lfep.jar');
import nlp.*;

ngram_size = 3;
n_folds = 2;
validateOnTestSet = true;
wordbased = true;

loadSettings;

trainingData = nlp.model.LabeledDataset();
trainingData.parse(trainingFile);

testData = nlp.model.LabeledDataset();
testData.parse(testFile);

unlabeledData = nlp.model.UnlabeledDataset();
unlabeledData.add(trainingData);
unlabeledData.add(testData);
if validateOnTestSet    
    unlabeledData.parse([],ulTargetData);
end 
myNgramModel = NgramModel(unlabeledData, trainingData);   
myNgramModel.setWordBased(wordbased);

if validateOnTestSet
    [train_features, train_labels, train_tokens] = myNgramModel.getTrainingSet(ngram_size, max(no_of_ngrams)+500);
    [test_features, test_labels, test_tokens, oovIndices] = myNgramModel.getTestSet(ngram_size, no_of_ngrams_test, testData);
else 
    [train_features, train_labels, train_tokens] = myNgramModel.getTrainingSet(ngram_size, max(no_of_ngrams));
end

%display('Writing out data...');
%writeOut(train_labels, train_features.values, 'train.txt');
%writeOut(test_labels(oovIndices), test_features.values(oovIndices,:), 'oov.txt');

tagMap = myNgramModel.getLabelsToTagMap();
clear myNgramModel;
NgramModel.printFeatureVector(train_features,train_tokens,1);

fprintf('No. of training examples: %d\n', size(train_features.values,1));
if (~validateOnTestSet)
    fprintf('SVM ');
    train(train_labels,train_features.values,sprintf('-q -v %d -B 1',n_folds));
    fprintf('Baseline: %f\n\n', NgramModel.majBaseline(train_labels)*100);
else
    display('Training SVM...');
    model = train(train_labels,train_features.values,'-q -B 1');
    [~,accuracy_train] = predict(train_labels, train_features.values ,model, '-q');
    [predicted_labels,accuracy_test] = predict(test_labels,test_features.values,model,'-q');
    
    [oovAcc,oovCounts] = NgramModel.unknownWordAccuracy(test_labels, predicted_labels,oovIndices);
    fprintf('Acc. on training set (all words): %f\n', accuracy_train(1));
    fprintf('Acc. on test set (all words): %f\n', accuracy_test(1));
    fprintf('Acc. on test set (unknown words): %f (%d / %d)\n', oovAcc*100, oovCounts.correct, oovCounts.total);
    fprintf('Unknown word baseline: %f\n\n', NgramModel.majBaseline(test_labels(oovIndices)*100));
    
    NgramModel.printErrors(tagMap, test_labels, predicted_labels,oovIndices, test_tokens);
end

end