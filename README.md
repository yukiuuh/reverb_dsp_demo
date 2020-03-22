Reverb DSP demo
====

Kotlinでデジタル信号処理

デジタル信号処理の講義の課題で作ったもの

## Description
畳み込みリバーブ

## Usage

IR設定→Source設定→再生

### IR設定

#### Load IR

Wav形式のインパルスレスポンスファイルを読み込み

#### Record IR

ボタンを押すとパルス音が出るのでマイクから反響音を録る

Time:録音するIRの長さ

Delay:録音し始めるまで遅延させる

SaveIRボタンを押すと録ったIRをWav形式で保存

### Source設定

リバーブをかけるための音源

#### Load Audio File

Wav形式の音声ファイルを読み込み

#### Record

押してから右のボックスの時間音声を録音

### 再生

Dry: Sourceになってる音声をそのまま再生
Wet: SourceにIR適用して再生
Stop: 再生停止