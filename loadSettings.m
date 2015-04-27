if (nargin == 0)
   targetDomain='emails';
end
trainingFile = {'../datasets/Google Task/source/ontonotes-wsj-train'};
testFile = {sprintf('../datasets/Google Task/target/%s/gweb-%s-dev',targetDomain, targetDomain)};
ulTargetData = {sprintf('../datasets/Google Task/target/%s/gweb-%s.unlabeled',targetDomain, targetDomain)};
lTargetData = {sprintf('../datasets/Google Task/target/%s/gweb-%s-test',targetDomain, targetDomain)};
ulSourceData = {'../datasets/wsj_ul.100k'};
%ulSourceData = {'../workspace/PythonTest/firstCorpus.txt'};

if strcmp(targetDomain,'bio')
   trainingFile = {'../datasets/train-wsj-02-21'};
   testFile = {'../datasets/onco_train.500'};
   ulTargetData = {'../datasets/input/biomed_ul.100k'};
   lTargetData = {'../datasets/input/onco_test.500'};
   ulSourceData = {'../datasets/input/wsj_ul.100k'};
end

ngram_size = 5; % corresponds to 2*w_s + 1
no_of_ngrams = 'all'; %'all'; why should set it to 1000
no_of_ngrams_test = 'all'; %'all';
saveToFile = false;

wordFeatures = cell(0);
%{}means the content, while()means a subcell
%.type is just the name of the Java class that you want to use, along with the initial arguments.
wordFeatures{end+1}.type = 'nlp.processing.features.DistFeatures(unlabeledData,500)';
%.alwaysActive  describes whether we will also use that feature for neighboring tokens of the current token v_0
wordFeatures{end}.alwaysActive = true; % is also active for neighbor tokens?
wordFeatures{end+1}.type = 'nlp.processing.features.MorphFeatures(trainingData)';
wordFeatures{end}.alwaysActive = true; % is also active for neighbor tokens?
wordFeatures{end+1}.type = 'nlp.processing.features.WordSignatures(trainingData)';
wordFeatures{end}.alwaysActive = true; % is also active for neighbor tokens?

addpath('liblinear');
