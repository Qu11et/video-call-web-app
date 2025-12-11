const API_BASE_URL = "/api/rooms";

export const createRoomApi = async (type: 'P2P' | 'GROUP' = 'P2P'): Promise<string | null> => {
  try {
    const response = await fetch(`${API_BASE_URL}/create`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ type }) // Gửi type lên server
    });
    
    if (response.ok) {
      const data = await response.json();
      return data.roomId;
    }
    return null;
  } catch (error) {
    console.error("Lỗi khi tạo phòng:", error);
    return null;
  }
};

export const checkRoomExistsApi = async (roomId: string): Promise<{ exists: boolean, type?: string }> => {
  try {
    const response = await fetch(`${API_BASE_URL}/${roomId}/join`, {
      method: "POST",
    });
    
    if (response.ok) {
      const data = await response.json();
      return { exists: true, type: data.type }; // Lấy type từ server trả về
    }
    return { exists: false };
  } catch (error) {
    console.error("Lỗi khi kiểm tra phòng:", error);
    return { exists: false };
  }
};