using System;
using System.IO;
using System.Media;
using System.Text;
using System.Threading.Tasks;

namespace clock.Core
{
    public static class AudioHelper
    {
        public static void PlaySound(string path, double volume)
        {
            Task.Run(() =>
            {
                try
                {
                    if (!File.Exists(path)) return;

                    // 限制音量範圍 0.0 ~ 1.0
                    if (volume < 0) volume = 0;
                    if (volume > 1) volume = 1;

                    // 如果音量是 1.0，直接播放原始檔案 (最快、最大聲)
                    if (Math.Abs(volume - 1.0) < 0.01)
                    {
                        using (var player = new SoundPlayer(path))
                        {
                            player.Play(); // Fire and forget
                        }
                        return;
                    }

                    // 如果音量需要調整，進行 PCM 振幅縮放
                    byte[] fileBytes = File.ReadAllBytes(path);
                    using (var ms = new MemoryStream(fileBytes))
                    using (var reader = new BinaryReader(ms))
                    using (var outStream = new MemoryStream())
                    using (var writer = new BinaryWriter(outStream))
                    {
                        // 1. 複製 Header (簡單假設標準 WAV Header 為 44 bytes)
                        // 更嚴謹的做法是搜尋 "data" chunk，但為了輕量化先做標準處理
                        int headerSize = 44;
                        
                        // 嘗試尋找 "data" chunk 以支援不同格式
                        ms.Position = 12; // Skip RIFF type
                        while (ms.Position < ms.Length)
                        {
                            byte[] chunkId = reader.ReadBytes(4);
                            int chunkSize = reader.ReadInt32();
                            if (Encoding.ASCII.GetString(chunkId) == "data")
                            {
                                headerSize = (int)ms.Position;
                                break;
                            }
                            ms.Position += chunkSize;
                        }

                        // 重置位置，複製 Header
                        ms.Position = 0;
                        writer.Write(reader.ReadBytes(headerSize));

                        // 2. 處理音訊資料 (假設 16-bit PCM)
                        // 如果不是 16-bit，這段可能會產生雜訊，但在大多數 notify.wav 中是通用的
                        while (ms.Position < ms.Length)
                        {
                            try
                            {
                                short sample = reader.ReadInt16();
                                // 調整振幅
                                sample = (short)(sample * volume);
                                writer.Write(sample);
                            }
                            catch (EndOfStreamException) 
                            { 
                                break; 
                            }
                        }

                        outStream.Position = 0;
                        using (var player = new SoundPlayer(outStream))
                        {
                            player.Play();
                        }
                    }
                }
                catch (Exception ex)
                {
                    System.Diagnostics.Debug.WriteLine($"Audio Error: {ex.Message}");
                    // 發生任何錯誤 (例如格式不支援)，直接降級播放原始檔案
                    try
                    {
                        using (var player = new SoundPlayer(path))
                        {
                            player.Play();
                        }
                    }
                    catch { }
                }
            });
        }
    }
}